package com.payment.rag.Config;

import com.alibaba.cloud.ai.memory.redis.JedisRedisChatMemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisMessage {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout:60000ms}")
    private java.time.Duration redisTimeout;

    /**
     * 用 Redis 存储对话历史
     * 使用 RedisBuilder 的 host/port/password API（spring-ai-alibaba 1.0.0.2）
     */
    @Bean
    public JedisRedisChatMemoryRepository redisChatMemoryRepository() {
        JedisRedisChatMemoryRepository.RedisBuilder builder = JedisRedisChatMemoryRepository.builder()
                .host(redisHost)
                .port(redisPort)
                .timeout((int) redisTimeout.toMillis());

        // 仅在密码非空时设置
        if (redisPassword != null && !redisPassword.isEmpty()) {
            builder.password(redisPassword);
        }

        return builder.build();
    }

}
