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
 * Redis common utilities based on Redisson.
 *
 * <p>In addition to basic KV/Hash/Set operations, this class provides
 * reusable cache protection helpers for:
 * <ul>
 *     <li>cache penetration: null marker cache</li>
 *     <li>cache breakdown: mutex rebuild</li>
 *     <li>cache avalanche: randomized TTL jitter</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class RedisUtils {

    private static final String NULL_VALUE = "__NULL__";
    private static final String CACHE_LOCK_PREFIX = "lock:cache:";
    private static final long DEFAULT_LOCK_WAIT_MILLIS = 200L;
    private static final long DEFAULT_LOCK_LEASE_SECONDS = 10L;

    private final RedissonClient redissonClient;

    public boolean exists(String key) {
        return redissonClient.getKeys().countExists(key) > 0;
    }

    public String get(String key) {
        return getBucket(key).get();
    }

    public void set(String key, String value) {
        getBucket(key).set(value);
    }

    public void set(String key, String value, Duration duration) {
        getBucket(key).set(value, duration);
    }

    public void set(String key, String value, long timeout, TimeUnit unit) {
        getBucket(key).set(value, timeout, unit);
    }

    public void setWithRandomTtl(String key, String value, Duration baseDuration, long randomSeconds) {
        getBucket(key).set(value, addRandomJitter(baseDuration, randomSeconds));
    }

    public <T> void setJson(String key, T value, Duration duration) {
        getBucket(key).set(JsonUtils.toJson(value), duration);
    }

    public <T> void setJsonWithRandomTtl(String key, T value, Duration baseDuration, long randomSeconds) {
        getBucket(key).set(JsonUtils.toJson(value), addRandomJitter(baseDuration, randomSeconds));
    }

    public <T> T getJson(String key, Class<T> clazz) {
        String value = getBucket(key).get();
        if (value == null || value.isBlank() || NULL_VALUE.equals(value)) {
            return null;
        }
        return JsonUtils.fromJson(value, clazz);
    }

    public boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        return getBucket(key).trySet(value, timeout, unit);
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        return redissonClient.getKeys().expire(key, timeout, unit);
    }

    public boolean delete(String key) {
        return redissonClient.getKeys().delete(key) > 0;
    }

    public long delete(String... keys) {
        if (keys == null || keys.length == 0) {
            return 0L;
        }
        return redissonClient.getKeys().delete(keys);
    }

    public Map<String, String> hashEntries(String key) {
        Map<String, String> map = getMap(key).readAllMap();
        return map == null ? Collections.emptyMap() : map;
    }

    public String hashGet(String key, String field) {
        return getMap(key).get(field);
    }

    public void hashPut(String key, String field, String value) {
        getMap(key).put(field, value);
    }

    public long hashRemove(String key, String... fields) {
        return getMap(key).fastRemove(fields);
    }

    public Set<String> setMembers(String key) {
        Set<String> members = getSet(key).readAll();
        return members == null ? Collections.emptySet() : members;
    }

    public int setSize(String key) {
        return getSet(key).size();
    }

    public boolean setAdd(String key, String value) {
        return getSet(key).add(value);
    }

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

    public boolean setRemove(String key, String value) {
        return getSet(key).remove(value);
    }

    public RLock getLock(String key) {
        return redissonClient.getLock(key);
    }

    public long incrementAndGet(String key) {
        return getAtomicLongRef(key).incrementAndGet();
    }

    public long incrementAndGet(String key, long timeout, TimeUnit unit) {
        RAtomicLong atomicLong = getAtomicLongRef(key);
        long value = atomicLong.incrementAndGet();
        if (value == 1L) {
            atomicLong.expire(timeout, unit);
        }
        return value;
    }

    public long getAtomicLong(String key) {
        return getAtomicLongRef(key).get();
    }

    public void setAtomicLong(String key, long value) {
        getAtomicLongRef(key).set(value);
    }

    public boolean bloomFilterTryInit(String key, long expectedInsertions, double falseProbability) {
        return getBloomFilter(key).tryInit(expectedInsertions, falseProbability);
    }

    public boolean bloomFilterAdd(String key, String value) {
        return getBloomFilter(key).add(value);
    }

    public boolean bloomFilterContains(String key, String value) {
        return getBloomFilter(key).contains(value);
    }

    public boolean bloomFilterExpire(String key, long timeout, TimeUnit unit) {
        return getBloomFilter(key).expire(timeout, unit);
    }

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


