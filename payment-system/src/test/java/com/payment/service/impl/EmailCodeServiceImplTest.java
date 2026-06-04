package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.config.EmailAuthProperties;
import com.payment.enums.EmailCodeSceneEnum;
import com.payment.util.RedisUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 邮箱验证码测试类，用于验证邮箱验证码相关逻辑。
 */
class EmailCodeServiceImplTest {

    /**
     * 发送编码ShouldStore编码AndSendMail。
     */
    @Test
    void sendCodeShouldStoreCodeAndSendMail() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        EmailAuthProperties properties = buildProperties();
        MailProperties mailProperties = buildMailProperties();
        when(redisUtils.exists("auth:email:cooldown:login:demo@test.com")).thenReturn(false);
        when(redisUtils.incrementAndGet("auth:email:daily:login:demo@test.com", 1L, TimeUnit.DAYS)).thenReturn(1L);

        EmailCodeServiceImpl service = new EmailCodeServiceImpl(mailSender, redisUtils, properties, mailProperties);
        service.sendCode("Demo@Test.com", EmailCodeSceneEnum.LOGIN);

        verify(redisUtils).set(eq("auth:email:code:login:demo@test.com"), any(String.class), eq(Duration.ofMinutes(10)));
        verify(redisUtils).set("auth:email:cooldown:login:demo@test.com", "1", Duration.ofSeconds(60));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mailMessage = captor.getValue();
        assertEquals("demo@test.com", mailMessage.getTo()[0]);
        assertEquals("noreply@test.com", mailMessage.getFrom());
        assertEquals("[SalesSystem]邮箱登录验证码", mailMessage.getSubject());
    }

    /**
     * 发送编码ShouldRejectFrequent请求。
     */
    @Test
    void sendCodeShouldRejectFrequentRequests() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        EmailAuthProperties properties = buildProperties();
        MailProperties mailProperties = buildMailProperties();
        when(redisUtils.exists("auth:email:cooldown:login:demo@test.com")).thenReturn(true);

        EmailCodeServiceImpl service = new EmailCodeServiceImpl(mailSender, redisUtils, properties, mailProperties);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.sendCode("demo@test.com", EmailCodeSceneEnum.LOGIN)
        );

        assertEquals("验证码发送过于频繁，请稍后再试", exception.getMessage());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    /**
     * 校验编码ShouldKeep编码WhenNotConsumed。
     */
    @Test
    void validateCodeShouldKeepCodeWhenNotConsumed() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        EmailCodeServiceImpl service = new EmailCodeServiceImpl(mailSender, redisUtils, buildProperties(), buildMailProperties());
        when(redisUtils.get("auth:email:code:recover:demo@test.com")).thenReturn("123456");

        service.validateCode("demo@test.com", "123456", EmailCodeSceneEnum.RECOVER, false);

        verify(redisUtils, never()).delete("auth:email:code:recover:demo@test.com");
    }

    /**
     * 校验编码ShouldDelete缓存WhenConsumed。
     */
    @Test
    void validateCodeShouldDeleteCacheWhenConsumed() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        EmailCodeServiceImpl service = new EmailCodeServiceImpl(mailSender, redisUtils, buildProperties(), buildMailProperties());
        when(redisUtils.get("auth:email:code:login:demo@test.com")).thenReturn("123456");

        service.validateCode("demo@test.com", "123456", EmailCodeSceneEnum.LOGIN, true);

        verify(redisUtils).delete("auth:email:code:login:demo@test.com");
    }

    /**
     * 构建配置。
     */
    private EmailAuthProperties buildProperties() {
        EmailAuthProperties properties = new EmailAuthProperties();
        properties.setEnabled(true);
        properties.setFrom("noreply@test.com");
        properties.setSubjectPrefix("[SalesSystem]");
        properties.setCodeTtlMinutes(10);
        properties.setSendCooldownSeconds(60);
        properties.setMaxDailySendCount(20);
        return properties;
    }

    /**
     * 构建Mail配置。
     */
    private MailProperties buildMailProperties() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setUsername("noreply@test.com");
        mailProperties.setPassword("mail-secret");
        return mailProperties;
    }
}
