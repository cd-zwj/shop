package com.payment.service.login.impl;

import com.payment.common.BusinessException;
import com.payment.entity.PlatformUser;
import com.payment.mapper.PlatformUserMapper;
import com.payment.service.SmsCodeService;
import com.payment.service.login.PlatformLoginRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 短信平台登录测试类，用于验证短信平台登录相关逻辑。
 */
class SmsPlatformLoginHandlerTest {

    /**
     * 判断是否需要AuthenticateActiveUserBySmsCode。
     */
    @Test
    void shouldAuthenticateActiveUserBySmsCode() {
        PlatformUserMapper platformUserMapper = mock(PlatformUserMapper.class);
        SmsCodeService smsCodeService = mock(SmsCodeService.class);
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setPhone("13800000000");
        user.setStatus(1);
        user.setDeleted(0);
        when(platformUserMapper.selectOne(any())).thenReturn(user);

        SmsPlatformLoginHandler handler = new SmsPlatformLoginHandler(platformUserMapper, smsCodeService);

        PlatformUser result = handler.authenticate(PlatformLoginRequest.sms("13800000000", "654321"));

        assertEquals(1L, result.getId());
        verify(smsCodeService).validateLoginCode("13800000000", "654321", true);
    }

    /**
     * 判断是否需要RejectMissingPhoneUser。
     */
    @Test
    void shouldRejectMissingPhoneUser() {
        PlatformUserMapper platformUserMapper = mock(PlatformUserMapper.class);
        SmsCodeService smsCodeService = mock(SmsCodeService.class);
        when(platformUserMapper.selectOne(any())).thenReturn(null);

        SmsPlatformLoginHandler handler = new SmsPlatformLoginHandler(platformUserMapper, smsCodeService);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> handler.authenticate(PlatformLoginRequest.sms("13800000000", "654321"))
        );

        assertEquals("手机号或验证码错误", exception.getMessage());
    }
}
