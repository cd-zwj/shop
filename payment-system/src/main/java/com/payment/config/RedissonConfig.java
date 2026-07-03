package com.payment.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置类，提供分布式锁、分布式集合等 Redis 高级功能的客户端。
 * <p>
 * 默认使用单机模式连接 Redis，通过 {@code spring.data.redis.*} 配置连接参数。
 * 仅当 {@code spring.redis.enabled=true}（默认 true）时才装配。
 * </p>
 */
@Configuration
@ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedissonConfig {
  /** Redis 主机地址，默认 localhost */
  @Value("${spring.data.redis.host:localhost}")
    private String redisHost;


    /** Redis 端口，默认 6379 */
    @Value("${spring.data.redis.port:6379}")
    private Integer redisPort;

    /** Redis 认证密码，为空则不设置 */
    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /** Redis 数据库索引，默认 0 */
    @Value("${spring.data.redis.database:0}")
    private Integer redisDatabase;

    /**
     * 创建 Redisson 客户端 Bean，使用单机模式连接 Redis。
     *
     * @return RedissonClient 实例
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 单机模式配置
        String address = "redis://" + redisHost + ":" + redisPort;
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisDatabase)
                .setConnectionPoolSize(64)
                .setConnectionMinimumIdleSize(10)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);
        
        // 如果有密码则设置密码
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }
        
        return Redisson.create(config);
    }
}
