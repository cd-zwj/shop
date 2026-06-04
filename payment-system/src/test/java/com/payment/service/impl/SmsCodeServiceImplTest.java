package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.config.SmsAuthProperties;
import com.payment.service.SmsCodeService;
import com.payment.util.RedisUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 短信验证码服务测试类，用于验证短信验证码相关逻辑。
 */
class SmsCodeServiceImplTest {

    /**
     * 判断是否需要SendLoginCodeWhenSmsEnabled。
     */
    @Test
    void shouldSendLoginCodeWhenSmsEnabled() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        SmsAuthProperties properties = buildEnabledProperties();
        when(redisUtils.exists(anyString())).thenReturn(false);
        when(redisUtils.incrementAndGet(anyString(), anyLong(), any())).thenReturn(1L);

        SmsCodeService service = new SmsCodeServiceImpl(redisUtils, properties);

        assertDoesNotThrow(() -> service.sendLoginCode("13800000000"));
        verify(redisUtils, times(2)).set(anyString(), anyString(), any(java.time.Duration.class));
    }

    /**
     * 判断是否需要RejectSendWhenSmsDisabled。
     */
    @Test
    void shouldRejectSendWhenSmsDisabled() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        SmsAuthProperties properties = new SmsAuthProperties();
        properties.setEnabled(false);
        SmsCodeService service = new SmsCodeServiceImpl(redisUtils, properties);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.sendLoginCode("13800000000"));

        assertEquals("短信能力未开启，请联系管理员配置", exception.getMessage());
    }

    /**
     * 判断是否需要ValidateAndConsumeLoginCode。
     */
    @Test
    void shouldValidateAndConsumeLoginCode() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        SmsAuthProperties properties = buildEnabledProperties();
        when(redisUtils.get("auth:sms:code:login:13800000000")).thenReturn("654321");

        SmsCodeService service = new SmsCodeServiceImpl(redisUtils, properties);

        assertDoesNotThrow(() -> service.validateLoginCode("13800000000", "654321", true));
        verify(redisUtils).delete("auth:sms:code:login:13800000000");
    }

    /**
     * 判断是否需要RejectWrongLoginCode。
     */
    @Test
    void shouldRejectWrongLoginCode() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        SmsAuthProperties properties = buildEnabledProperties();
        when(redisUtils.get("auth:sms:code:login:13800000000")).thenReturn("654321");

        SmsCodeService service = new SmsCodeServiceImpl(redisUtils, properties);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validateLoginCode("13800000000", "123456", true)
        );

        assertEquals("短信验证码错误", exception.getMessage());
        verify(redisUtils, never()).delete(anyString());
    }

    /**
     * 构建启用配置。
     */
    private SmsAuthProperties buildEnabledProperties() {
        SmsAuthProperties properties = new SmsAuthProperties();
        properties.setEnabled(true);
        properties.setProvider("mock");
        properties.setCodeTtlMinutes(10);
        properties.setSendCooldownSeconds(60);
        properties.setMaxDailySendCount(20);
        return properties;
    }
}
