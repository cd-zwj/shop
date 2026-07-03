package com.payment.util;

import com.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 通用工具类，基于 Redisson 封装。
 * <p>
 * 除基础的 KV/Hash/Set 操作外，还提供缓存防护三件套：
 * <ul>
 *   <li><b>缓存穿透</b>：空值标记缓存（null marker）</li>
 *   <li><b>缓存击穿</b>：互斥锁重建（mutex rebuild）</li>
 *   <li><b>缓存雪崩</b>：随机 TTL 抖动（TTL jitter）</li>
 * </ul>
 * </p>
 */
@Component
@RequiredArgsConstructor
public class RedisUtils {

    /** 空值标记，用于防止缓存穿透 */
    private static final String NULL_VALUE = "__NULL__";
    /** 缓存互斥锁前缀 */
    private static final String CACHE_LOCK_PREFIX = "lock:cache:";
    /** 默认锁等待时间（毫秒） */
    private static final long DEFAULT_LOCK_WAIT_MILLIS = 200L;
    /** 默认锁持有时间（秒） */
    private static final long DEFAULT_LOCK_LEASE_SECONDS = 10L;

    /** Redisson 客户端 */
    private final RedissonClient redissonClient;

    /**
     * 判断 key 是否存在。
     *
     * @param key Redis 键
     * @return 存在返回 true，否则返回 false
     */
    public boolean exists(String key) {
        return redissonClient.getKeys().countExists(key) > 0;
    }

    /**
     * 获取 key 对应的值。
     *
     * @param key Redis 键
     * @return 值，不存在时返回 null
     */
    public String get(String key) {
        return getBucket(key).get();
    }

    /**
     * 设置 key-value（无过期时间）。
     *
     * @param key   Redis 键
     * @param value 值
     */
    public void set(String key, String value) {
        getBucket(key).set(value);
    }

    /**
     * 设置 key-value，指定过期时长。
     *
     * @param key      Redis 键
     * @param value    值
     * @param duration 过期时长
     */
    public void set(String key, String value, Duration duration) {
        getBucket(key).set(value, duration);
    }

    /**
     * 设置 key-value，指定过期时间和时间单位。
     *
     * @param key     Redis 键
     * @param value   值
     * @param timeout 过期时间数值
     * @param unit    时间单位
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        getBucket(key).set(value, timeout, unit);
    }

    /**
     * 设置 key-value，在基础时长上增加随机抖动，防止缓存雪崩。
     *
     * @param key           Redis 键
     * @param value         值
     * @param baseDuration  基础过期时长
     * @param randomSeconds 随机抖动秒数上限
     */
    public void setWithRandomTtl(String key, String value, Duration baseDuration, long randomSeconds) {
        getBucket(key).set(value, addRandomJitter(baseDuration, randomSeconds));
    }

    /**
     * 将对象序列化为 JSON 后存入 Redis。
     *
     * @param <T>      对象类型
     * @param key      Redis 键
     * @param value    对象
     * @param duration 过期时长
     */
    public <T> void setJson(String key, T value, Duration duration) {
        getBucket(key).set(JsonUtils.toJson(value), duration);
    }

    /**
     * 将对象序列化为 JSON 后存入 Redis，带随机 TTL 抖动。
     *
     * @param <T>           对象类型
     * @param key           Redis 键
     * @param value         对象
     * @param baseDuration  基础过期时长
     * @param randomSeconds 随机抖动秒数上限
     */
    public <T> void setJsonWithRandomTtl(String key, T value, Duration baseDuration, long randomSeconds) {
        getBucket(key).set(JsonUtils.toJson(value), addRandomJitter(baseDuration, randomSeconds));
    }

    /**
     * 从 Redis 获取 JSON 字符串并反序列化为指定类型。
     *
     * @param <T>   目标类型
     * @param key   Redis 键
     * @param clazz 目标类
     * @return 反序列化后的对象，值为空或为空值标记时返回 null
     */
    public <T> T getJson(String key, Class<T> clazz) {
        String value = getBucket(key).get();
        if (value == null || value.isBlank() || NULL_VALUE.equals(value)) {
            return null;
        }
        return JsonUtils.fromJson(value, clazz);
    }

    /**
     * 仅当 key 不存在时设置值（原子操作）。
     *
     * @param key     Redis 键
     * @param value   值
     * @param timeout 过期时间数值
     * @param unit    时间单位
     * @return 设置成功返回 true，key 已存在返回 false
     */
    public boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        return getBucket(key).trySet(value, timeout, unit);
    }

    /**
     * 设置 key 的过期时间。
     *
     * @param key     Redis 键
     * @param timeout 过期时间数值
     * @param unit    时间单位
     * @return 设置成功返回 true
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return redissonClient.getKeys().expire(key, timeout, unit);
    }

    /**
     * 删除单个 key。
     *
     * @param key Redis 键
     * @return 删除成功返回 true
     */
    public boolean delete(String key) {
        return redissonClient.getKeys().delete(key) > 0;
    }

    /**
     * 批量删除 key。
     *
     * @param keys Redis 键数组
     * @return 成功删除的 key 数量
     */
    public long delete(String... keys) {
        if (keys == null || keys.length == 0) {
            return 0L;
        }
        return redissonClient.getKeys().delete(keys);
    }

    /**
     * 获取 Hash 中所有字段和值。
     *
     * @param key Redis 键
     * @return 字段-值映射，不存在时返回空 Map
     */
    public Map<String, String> hashEntries(String key) {
        Map<String, String> map = getMap(key).readAllMap();
        return map == null ? Collections.emptyMap() : map;
    }

    /**
     * 获取 Hash 中指定字段的值。
     *
     * @param key   Redis 键
     * @param field Hash 字段名
     * @return 字段值，不存在时返回 null
     */
    public String hashGet(String key, String field) {
        return getMap(key).get(field);
    }

    /**
     * 向 Hash 中设置字段值。
     *
     * @param key   Redis 键
     * @param field Hash 字段名
     * @param value 字段值
     */
    public void hashPut(String key, String field, String value) {
        getMap(key).put(field, value);
    }

    /**
     * 删除 Hash 中的指定字段。
     *
     * @param key    Redis 键
     * @param fields 要删除的字段名数组
     * @return 成功删除的字段数量
     */
    public long hashRemove(String key, String... fields) {
        return getMap(key).fastRemove(fields);
    }

    /**
     * 获取 Set 中所有成员。
     *
     * @param key Redis 键
     * @return 成员集合，不存在时返回空 Set
     */
    public Set<String> setMembers(String key) {
        Set<String> members = getSet(key).readAll();
        return members == null ? Collections.emptySet() : members;
    }

    /**
     * 获取 Set 的成员数量。
     *
     * @param key Redis 键
     * @return 成员数量
     */
    public int setSize(String key) {
        return getSet(key).size();
    }

    /**
     * 向 Set 中添加单个成员。
     *
     * @param key   Redis 键
     * @param value 成员值
     * @return 添加成功返回 true，已存在返回 false
     */
    public boolean setAdd(String key, String value) {
        return getSet(key).add(value);
    }

    /**
     * 向 Set 中批量添加成员。
     *
     * @param key    Redis 键
     * @param values 成员值集合
     * @return 成功添加的成员数量
     */
    public long setAdd(String key, Set<String> values) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        long count = 0L;
        for (String value : values) {
            if (getSet(key).add(value)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 从 Set 中移除指定成员。
     *
     * @param key   Redis 键
     * @param value 要移除的成员值
     * @return 移除成功返回 true
     */
    public boolean setRemove(String key, String value) {
        return getSet(key).remove(value);
    }

    /**
     * 获取分布式锁实例。
     *
     * @param key 锁的 key
     * @return RLock 实例
     */
    public RLock getLock(String key) {
        return redissonClient.getLock(key);
    }

    /**
     * 原子递增并返回新值。
     *
     * @param key Redis 键
     * @return 递增后的新值
     */
    public long incrementAndGet(String key) {
        return getAtomicLongRef(key).incrementAndGet();
    }

    /**
     * 原子递增并返回新值，首次设置时自动添加过期时间。
     * <p>
     * 用于限流计数器场景：首次递增时设置过期时间实现滑动窗口。
     *
     * @param key     Redis 键
     * @param timeout 过期时间数值
     * @param unit    时间单位
     * @return 递增后的新值
     */
    public long incrementAndGet(String key, long timeout, TimeUnit unit) {
        RAtomicLong atomicLong = getAtomicLongRef(key);
        long value = atomicLong.incrementAndGet();
        if (value == 1L) {
            atomicLong.expire(timeout, unit);
        }
        return value;
    }

    /**
     * 获取原子计数器的当前值。
     *
     * @param key Redis 键
     * @return 当前值
     */
    public long getAtomicLong(String key) {
        return getAtomicLongRef(key).get();
    }

    /**
     * 设置原子计数器的值。
     *
     * @param key   Redis 键
     * @param value 要设置的值
     */
    public void setAtomicLong(String key, long value) {
        getAtomicLongRef(key).set(value);
    }

    /**
     * 初始化布隆过滤器。
     *
     * @param key                布隆过滤器 key
     * @param expectedInsertions 预期插入元素数量
     * @param falseProbability   误判概率
     * @return 初始化成功返回 true
     */
    public boolean bloomFilterTryInit(String key, long expectedInsertions, double falseProbability) {
        return getBloomFilter(key).tryInit(expectedInsertions, falseProbability);
    }

    /**
     * 向布隆过滤器中添加元素。
     *
     * @param key   布隆过滤器 key
     * @param value 元素值
     * @return 添加成功返回 true
     */
    public boolean bloomFilterAdd(String key, String value) {
        return getBloomFilter(key).add(value);
    }

    /**
     * 判断布隆过滤器中是否可能存在该元素。
     *
     * @param key   布隆过滤器 key
     * @param value 元素值
     * @return 可能存在返回 true（有误判概率），一定不存在返回 false
     */
    public boolean bloomFilterContains(String key, String value) {
        return getBloomFilter(key).contains(value);
    }

    /**
     * 设置布隆过滤器的过期时间。
     *
     * @param key     布隆过滤器 key
     * @param timeout 过期时间数值
     * @param unit    时间单位
     * @return 设置成功返回 true
     */
    public boolean bloomFilterExpire(String key, long timeout, TimeUnit unit) {
        return getBloomFilter(key).expire(timeout, unit);
    }

    /**
     * 缓存穿透防护：查询带空值标记缓存。
     * <p>
     * 流程：查缓存 → 命中空标记返回 null → 命中数据返回 → 未命中查数据库 → 结果写入缓存。
     * 数据库返回 null 时写入空值标记（防穿透），非空数据带随机 TTL（防雪崩）。
     *
     * @param <T>            目标类型
     * @param key            Redis 键
     * @param clazz          目标类
     * @param cacheDuration  正常数据缓存时长
     * @param nullDuration   空值标记缓存时长
     * @param randomSeconds  随机 TTL 抖动秒数
     * @param dbFallback     数据库查询回调
     * @return 查询结果，不存在时返回 null
     */
    public <T> T queryWithPassThrough(String key,
                                      Class<T> clazz,
                                      Duration cacheDuration,
                                      Duration nullDuration,
                                      long randomSeconds,
                                      Supplier<T> dbFallback) {
        String cachedValue = getBucket(key).get();
        if (cachedValue != null) {
            if (NULL_VALUE.equals(cachedValue)) {
                return null;
            }
            return JsonUtils.fromJson(cachedValue, clazz);
        }

        T dbResult = dbFallback.get();
        if (dbResult == null) {
            getBucket(key).set(NULL_VALUE, nullDuration);
            return null;
        }

        setJsonWithRandomTtl(key, dbResult, cacheDuration, randomSeconds);
        return dbResult;
    }

    /**
     * 缓存击穿防护：互斥锁重建查询。
     * <p>
     * 流程：查缓存 → 命中返回 → 未命中尝试获取分布式锁 → 获取成功后 double check 缓存 →
     * 仍未命中则查数据库并写入缓存 → 释放锁。获取锁失败时退避 50ms 后降级为穿透防护查询。
     * </p>
     *
     * @param <T>            目标类型
     * @param key            Redis 键
     * @param clazz          目标类
     * @param cacheDuration  正常数据缓存时长
     * @param nullDuration   空值标记缓存时长
     * @param randomSeconds  随机 TTL 抖动秒数
     * @param dbFallback     数据库查询回调
     * @return 查询结果，不存在时返回 null
     */
    public <T> T queryWithMutex(String key,
                                Class<T> clazz,
                                Duration cacheDuration,
                                Duration nullDuration,
                                long randomSeconds,
                                Supplier<T> dbFallback) {
        String cachedValue = getBucket(key).get();
        if (cachedValue != null) {
            if (NULL_VALUE.equals(cachedValue)) {
                return null;
            }
            return JsonUtils.fromJson(cachedValue, clazz);
        }

        String lockKey = CACHE_LOCK_PREFIX + key;
        RLock lock = getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(DEFAULT_LOCK_WAIT_MILLIS, DEFAULT_LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                sleepQuietly(50L);
                return queryWithPassThrough(key, clazz, cacheDuration, nullDuration, randomSeconds, dbFallback);
            }

            String doubleCheckValue = getBucket(key).get();
            if (doubleCheckValue != null) {
                if (NULL_VALUE.equals(doubleCheckValue)) {
                    return null;
                }
                return JsonUtils.fromJson(doubleCheckValue, clazz);
            }

            T dbResult = dbFallback.get();
            if (dbResult == null) {
                getBucket(key).set(NULL_VALUE, nullDuration);
                return null;
            }

            setJsonWithRandomTtl(key, dbResult, cacheDuration, randomSeconds);
            return dbResult;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取缓存锁被中断", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 判断给定的值是否为空值标记（NULL_VALUE）。
     * <p>
     * 空值标记用于缓存穿透防护，当数据库查询结果为 null 时写入该标记，
     * 后续查询命中标记后直接返回 null，避免反复查询数据库。
     * </p>
     *
     * @param value 待检查的缓存值
     * @return 是空值标记返回 true，否则返回 false
     */
    public boolean isNullMarker(String value) {
        return NULL_VALUE.equals(value);
    }

    private RBucket<String> getBucket(String key) {
        return redissonClient.getBucket(key, StringCodec.INSTANCE);
    }

    private RMap<String, String> getMap(String key) {
        return redissonClient.getMap(key, StringCodec.INSTANCE);
    }

    private RSet<String> getSet(String key) {
        return redissonClient.getSet(key, StringCodec.INSTANCE);
    }

    private RAtomicLong getAtomicLongRef(String key) {
        return redissonClient.getAtomicLong(key);
    }

    private RBloomFilter<String> getBloomFilter(String key) {
        return redissonClient.getBloomFilter(key);
    }

    private Duration addRandomJitter(Duration baseDuration, long randomSeconds) {
        if (randomSeconds <= 0) {
            return baseDuration;
        }
        long seconds = ThreadLocalRandom.current().nextLong(randomSeconds + 1);
        return baseDuration.plusSeconds(seconds);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


