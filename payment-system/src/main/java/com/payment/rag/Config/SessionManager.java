package com.payment.rag.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * 会话管理工具类
 * 使用Redis zset保管用户会话列表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionManager {

    private final StringRedisTemplate redisTemplate;

    // 会话列表key前缀
    private static final String SESSION_LIST_PREFIX = "user:sessions:";
    // 会话信息key前缀
    private static final String SESSION_INFO_PREFIX = "session:info:";

    /**
     * 创建新会话
     * @param userId 用户ID
     * @return 会话ID
     */
    public String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        String sessionKey = SESSION_LIST_PREFIX + userId;

        // 添加到用户会话列表，score为当前时间戳
        redisTemplate.opsForZSet().add(sessionKey, sessionId, System.currentTimeMillis());

        // 设置会话信息（可选，存储会话创建时间等）
        String sessionInfoKey = SESSION_INFO_PREFIX + sessionId;
        redisTemplate.opsForHash().put(sessionInfoKey, "userId", userId);
        redisTemplate.opsForHash().put(sessionInfoKey, "createdAt", String.valueOf(System.currentTimeMillis()));

        log.info("已为用户 {} 创建新会话: {}", userId, sessionId);
        return sessionId;
    }

    /**
     * 获取用户的会话列表
     * @param userId 用户ID
     * @return 会话ID集合，按创建时间倒序排列
     */
    public Set<String> getUserSessions(String userId) {
        String sessionKey = SESSION_LIST_PREFIX + userId;
        Set<String> sessions = redisTemplate.opsForZSet().reverseRange(sessionKey, 0, -1);

        return sessions != null ? sessions : Collections.emptySet();
    }

    /**
     * 删除会话
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    public void deleteSession(String userId, String sessionId) {
        String sessionKey = SESSION_LIST_PREFIX + userId;

        // 1. 从会话列表中移除
        redisTemplate.opsForZSet().remove(sessionKey, sessionId);

        // 2. 删除会话信息
        String sessionInfoKey = SESSION_INFO_PREFIX + sessionId;
        redisTemplate.delete(sessionInfoKey);

        // 3. 清理对话历史（关键修改：删除 Redis 中的对话消息）
        // RedisChatMemoryRepository 存储格式：spring:ai:chat:memory:{conversationId}
        String chatMemoryKey = "spring:ai:chat:memory:" + sessionId;
        try {
            Boolean deleted = redisTemplate.delete(chatMemoryKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("已删除会话 {} 的聊天记忆", sessionId);
            } else {
                log.warn("未找到会话 {} 的聊天记忆键", sessionId);
            }
        } catch (Exception e) {
            log.error("删除会话 {} 的聊天记忆失败", sessionId, e);
        }

        log.info("已为用户 {} 删除会话: {}", userId, sessionId);
    }

    /**
     * 获取会话的用户ID
     * @param sessionId 会话ID
     * @return 用户ID
     */
    public String getUserIdBySession(String sessionId) {
        String sessionInfoKey = SESSION_INFO_PREFIX + sessionId;
        Object userId = redisTemplate.opsForHash().get(sessionInfoKey, "userId");
        return userId != null ? userId.toString() : null;
    }

    /**
     * 检查会话是否存在
     * @param sessionId 会话ID
     * @return 是否存在
     */
    public boolean sessionExists(String sessionId) {
        String sessionInfoKey = SESSION_INFO_PREFIX + sessionId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(sessionInfoKey));
    }

    /**
     * 更新会话最后活跃时间
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    public void updateSessionActivity(String userId, String sessionId) {
        String sessionKey = SESSION_LIST_PREFIX + userId;
        // 修复：使用 add 替换 score，而不是 incrementScore 累加
        redisTemplate.opsForZSet().add(sessionKey, sessionId, (double) System.currentTimeMillis());
    }

    /**
     * 清理过期会话（可选功能）
     * @param expireTimeMs 过期时间（毫秒）
     */
    public void cleanupExpiredSessions(long expireTimeMs) {
        // 实现清理逻辑，可以根据需要添加
        // 这里暂时留空，后续可以实现定时清理任务
    }
}