package com.payment.config;

import com.payment.util.RedisUtils;
import org.mockito.Mockito;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 测试环境用 mock 替代真实中间件连接（Redis、RabbitMQ、邮件等）。
 */
@TestConfiguration
public class TestRedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        RedissonClient mock = Mockito.mock(RedissonClient.class);
        // Sa-Token 内部调用 redissonClient.getConfig() 来判断是否集群模式
        // 返回一个真实的单机 Config 避免 NullPointerException
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        when(mock.getConfig()).thenReturn(config);

        // 让 getAtomicLong 返回安全的 mock，避免 RateLimitAspect NPE
        RAtomicLong atomicLong = Mockito.mock(RAtomicLong.class);
        when(atomicLong.incrementAndGet()).thenReturn(1L);
        when(atomicLong.get()).thenReturn(0L);
        when(mock.getAtomicLong(anyString())).thenReturn(atomicLong);

        return mock;
    }

    @Bean
    @Primary
    public ConnectionFactory testConnectionFactory() {
        return Mockito.mock(ConnectionFactory.class);
    }
    @Bean
    @Primary
    public RabbitTemplate testRabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);
    }

    @Bean
    @Primary
    public JavaMailSender testJavaMailSender() {
        return Mockito.mock(JavaMailSender.class);
    }

    @Bean
    @Primary
    public MailProperties testMailProperties() {
        return new MailProperties();
    }
}
