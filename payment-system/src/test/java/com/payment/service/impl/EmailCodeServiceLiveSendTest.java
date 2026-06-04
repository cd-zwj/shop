package com.payment.service.impl;

import com.payment.config.EmailAuthProperties;
import com.payment.enums.EmailCodeSceneEnum;
import com.payment.util.RedisUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 邮箱验证码服务LiveSend测试类，用于验证邮箱验证码服务LiveSend相关逻辑。
 */
class EmailCodeServiceLiveSendTest {

    /**
     * 判断是否需要SendReal邮箱按Backend服务。
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "MAIL_TEST_TO", matches = ".+")
    void shouldSendRealEmailByBackendService() {
        String username = requireEnv("MAIL_USERNAME");
        String password = requireEnv("MAIL_PASSWORD");
        String from = readEnvOrDefault("MAIL_FROM", username);
        String to = requireEnv("MAIL_TEST_TO");

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(readEnvOrDefault("MAIL_HOST", "smtp.qq.com"));
        mailSender.setPort(Integer.parseInt(readEnvOrDefault("MAIL_PORT", "465")));
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setDefaultEncoding("UTF-8");

        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.connectiontimeout", "5000");
        properties.put("mail.smtp.timeout", "5000");
        properties.put("mail.smtp.writetimeout", "5000");

        RedisUtils redisUtils = mock(RedisUtils.class);
        when(redisUtils.exists("auth:email:cooldown:login:" + to.toLowerCase())).thenReturn(false);
        when(redisUtils.incrementAndGet("auth:email:daily:login:" + to.toLowerCase(), 1L, TimeUnit.DAYS)).thenReturn(1L);

        EmailAuthProperties emailAuthProperties = new EmailAuthProperties();
        emailAuthProperties.setEnabled(true);
        emailAuthProperties.setFrom(from);
        emailAuthProperties.setSubjectPrefix("[SalesSystem]");
        emailAuthProperties.setCodeTtlMinutes(10);
        emailAuthProperties.setSendCooldownSeconds(60);
        emailAuthProperties.setMaxDailySendCount(20);

        MailProperties mailProperties = new MailProperties();
        mailProperties.setUsername(username);
        mailProperties.setPassword(password);

        EmailCodeServiceImpl service = new EmailCodeServiceImpl(mailSender, redisUtils, emailAuthProperties, mailProperties);
        service.sendCode(to, EmailCodeSceneEnum.LOGIN);
    }

    /**
     * 处理requireEnv。
     */
    private String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + key);
        }
        return value.trim();
    }

    /**
     * 处理readEnvOr默认值。
     */
    private String readEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
