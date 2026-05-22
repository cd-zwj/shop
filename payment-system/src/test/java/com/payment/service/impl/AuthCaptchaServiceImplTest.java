package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.dto.LoginCaptchaVO;
import com.payment.util.RedisUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthCaptchaServiceImplTest {

    @Test
    void generateCaptchaShouldStoreCodeReturnImageAndTrackBloomFilter() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        AuthCaptchaServiceImpl service = new AuthCaptchaServiceImpl(redisUtils);
        when(redisUtils.incrementAndGet("auth:captcha:rate:127_0_0_1", 60L, TimeUnit.SECONDS)).thenReturn(1L);

        LoginCaptchaVO result = service.generateCaptcha("127.0.0.1");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisUtils).set(keyCaptor.capture(), valueCaptor.capture(), eq(Duration.ofMinutes(5)));
        verify(redisUtils).bloomFilterTryInit("auth:captcha:bloom", 200000L, 0.01D);
        verify(redisUtils).bloomFilterExpire("auth:captcha:bloom", 48L, TimeUnit.HOURS);
        verify(redisUtils).bloomFilterAdd(eq("auth:captcha:bloom"), eq(result.getCaptchaKey()));

        assertNotNull(result.getCaptchaKey());
        assertTrue(keyCaptor.getValue().startsWith("auth:captcha:"));
        assertEquals(4, valueCaptor.getValue().length());
        assertTrue(result.getCaptchaImage().startsWith("data:image/png;base64,"));
    }

    @Test
    void generateCaptchaShouldRejectTooManyRequestsFromSameIp() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        AuthCaptchaServiceImpl service = new AuthCaptchaServiceImpl(redisUtils);
        when(redisUtils.incrementAndGet("auth:captcha:rate:10_0_0_1", 60L, TimeUnit.SECONDS)).thenReturn(11L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.generateCaptcha("10.0.0.1")
        );

        assertEquals("获取验证码过于频繁，请1分钟后再试", exception.getMessage());
    }

    @Test
    void validateCaptchaShouldPassWhenCodeMatchesIgnoringCase() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        AuthCaptchaServiceImpl service = new AuthCaptchaServiceImpl(redisUtils);
        when(redisUtils.bloomFilterContains("auth:captcha:bloom", "abc123")).thenReturn(true);
        when(redisUtils.get("auth:captcha:abc123")).thenReturn("ABCD");

        service.validateCaptcha("abc123", "abCd");

        verify(redisUtils).delete("auth:captcha:abc123");
    }

    @Test
    void validateCaptchaShouldRejectWrongCodeAndDeleteCache() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        AuthCaptchaServiceImpl service = new AuthCaptchaServiceImpl(redisUtils);
        when(redisUtils.bloomFilterContains("auth:captcha:bloom", "wrong1")).thenReturn(true);
        when(redisUtils.get("auth:captcha:wrong1")).thenReturn("ABCD");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validateCaptcha("wrong1", "ZZZZ")
        );

        verify(redisUtils).delete("auth:captcha:wrong1");
        assertEquals("验证码错误，请重新获取", exception.getMessage());
    }

    @Test
    void validateCaptchaShouldRejectExpiredCode() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        AuthCaptchaServiceImpl service = new AuthCaptchaServiceImpl(redisUtils);
        when(redisUtils.bloomFilterContains("auth:captcha:bloom", "expired1")).thenReturn(true);
        when(redisUtils.get(anyString())).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validateCaptcha("expired1", "ABCD")
        );

        verify(redisUtils).delete("auth:captcha:expired1");
        assertEquals("验证码已过期，请重新获取", exception.getMessage());
    }

    @Test
    void validateCaptchaShouldRejectUnknownCaptchaKeyBeforeRedisLookup() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        AuthCaptchaServiceImpl service = new AuthCaptchaServiceImpl(redisUtils);
        when(redisUtils.bloomFilterContains("auth:captcha:bloom", "missing1")).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validateCaptcha("missing1", "ABCD")
        );

        assertEquals("验证码不存在或已失效", exception.getMessage());
    }
}
