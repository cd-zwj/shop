package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.config.SmsAuthProperties;
import com.payment.service.SmsCodeService;
import com.payment.service.sms.SmsSender;
import com.payment.util.RedisUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 短信验证码服务测试类，用于验证短信验证码相关逻辑。
 */
@ExtendWith(MockitoExtension.class)
class SmsCodeServiceImplTest {

    @Mock
    private SmsSender smsSender;

    /**
     * 判断是否需要SendLoginCodeWhenSmsEnabled。
     */
    @Test
    @DisplayName("短信开启时应发送验证码并调用 SmsSender.send")
    void shouldSendLoginCodeWhenSmsEnabled() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        SmsAuthProperties properties = buildEnabledProperties();
        when(redisUtils.exists(anyString())).thenReturn(false);
        when(redisUtils.incrementAndGet(anyString(), anyLong(), any())).thenReturn(1L);

        SmsCodeService service = new SmsCodeServiceImpl(redisUtils, properties, smsSender);

        assertDoesNotThrow(() -> service.sendLoginCode("13800000000"));
        verify(redisUtils, times(2)).set(anyString(), anyString(), any(Duration.class));
        verify(smsSender).send(eq("13800000000"), anyString());
    }

    /**
     * 判断是否需要RejectSendWhenSmsDisabled。
     */
    @Test
    @DisplayName("短信未开启时应拒绝发送")
    void shouldRejectSendWhenSmsDisabled() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        SmsAuthProperties properties = new SmsAuthProperties();
        properties.setEnabled(false);
        SmsCodeService service = new SmsCodeServiceImpl(redisUtils, properties, smsSender);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.sendLoginCode("13800000000"));

        assertEquals("短信能力未开启，请联系管理员配置", exception.getMessage());
        verify(smsSender, never()).send(anyString(), anyString());
    }

    /**
     * 判断是否需要ValidateAndConsumeLoginCode。
     */
    @Test
    @DisplayName("验证码正确时应验证并消费")
    void shouldValidateAndConsumeLoginCode() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        SmsAuthProperties properties = buildEnabledProperties();
        when(redisUtils.get("auth:sms:code:login:13800000000")).thenReturn("654321");

        SmsCodeService service = new SmsCodeServiceImpl(redisUtils, properties, smsSender);

        assertDoesNotThrow(() -> service.validateLoginCode("13800000000", "654321", true));
        verify(redisUtils).delete("auth:sms:code:login:13800000000");
    }

    /**
     * 判断是否需要RejectWrongLoginCode。
     */
    @Test
    @DisplayName("验证码错误时应拒绝并报错")
    void shouldRejectWrongLoginCode() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        SmsAuthProperties properties = buildEnabledProperties();
        when(redisUtils.get("auth:sms:code:login:13800000000")).thenReturn("654321");

        SmsCodeService service = new SmsCodeServiceImpl(redisUtils, properties, smsSender);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validateLoginCode("13800000000", "123456", true)
        );

        assertEquals("短信验证码错误", exception.getMessage());
        verify(redisUtils, never()).delete(anyString());
    }

    @Test
    @DisplayName("SmsSender 抛异常时应包装为 BusinessException")
    void shouldWrapSenderExceptionAsBusinessException() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        SmsAuthProperties properties = buildEnabledProperties();
        when(redisUtils.exists(anyString())).thenReturn(false);
        when(redisUtils.incrementAndGet(anyString(), anyLong(), any())).thenReturn(1L);
        doThrow(new RuntimeException("供应商异常")).when(smsSender).send(anyString(), anyString());

        SmsCodeService service = new SmsCodeServiceImpl(redisUtils, properties, smsSender);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.sendLoginCode("13800000000")
        );

        assertEquals("短信发送失败，请稍后重试", exception.getMessage());
    }

    @Test
    @DisplayName("验证码过期时应报错")
    void shouldRejectWhenCodeExpired() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        SmsAuthProperties properties = buildEnabledProperties();
        when(redisUtils.get("auth:sms:code:login:13800000000")).thenReturn(null);

        SmsCodeService service = new SmsCodeServiceImpl(redisUtils, properties, smsSender);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validateLoginCode("13800000000", "654321", true)
        );

        assertEquals("短信验证码已过期，请重新获取", exception.getMessage());
    }

    @Test
    @DisplayName("coolDown 期间应拒绝发送")
    void shouldRejectSendDuringCooldown() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        SmsAuthProperties properties = buildEnabledProperties();
        when(redisUtils.exists(anyString())).thenReturn(true);

        SmsCodeService service = new SmsCodeServiceImpl(redisUtils, properties, smsSender);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.sendLoginCode("13800000000")
        );

        assertEquals("验证码发送过于频繁，请稍后再试", exception.getMessage());
        verify(smsSender, never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("每日发送次数超限时应拒绝发送")
    void shouldRejectWhenDailyLimitExceeded() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        SmsAuthProperties properties = buildEnabledProperties();
        when(redisUtils.exists(anyString())).thenReturn(false);
        when(redisUtils.incrementAndGet(anyString(), anyLong(), any())).thenReturn(21L);

        SmsCodeService service = new SmsCodeServiceImpl(redisUtils, properties, smsSender);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.sendLoginCode("13800000000")
        );

        assertEquals("今日验证码发送次数已达上限，请明天再试", exception.getMessage());
        verify(smsSender, never()).send(anyString(), anyString());
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
