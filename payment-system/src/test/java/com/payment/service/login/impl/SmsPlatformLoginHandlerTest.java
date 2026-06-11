package com.payment.service.login.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.entity.PlatformUser;
import com.payment.mapper.PlatformUserMapper;
import com.payment.service.SmsCodeService;
import com.payment.service.login.PlatformLoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 短信平台登录处理器测试类，用于验证短信平台登录相关逻辑。
 */
@ExtendWith(MockitoExtension.class)
class SmsPlatformLoginHandlerTest {

    @Mock
    private SmsCodeService smsCodeService;

    @Mock
    private PlatformUserMapper platformUserMapper;

    @Test
    @DisplayName("正常短信登录应返回 PlatformUser")
    void shouldAuthenticateBySmsSuccessfully() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setPhone("13800000000");
        user.setStatus(1);
        user.setDeleted(0);

        doNothing().when(smsCodeService).validateLoginCode("13800000000", "654321", true);
        when(platformUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        SmsPlatformLoginHandler handler = new SmsPlatformLoginHandler(smsCodeService, platformUserMapper);
        PlatformUser result = handler.authenticate(PlatformLoginRequest.sms("13800000000", "654321"));

        assertEquals(1L, result.getId());
        verify(smsCodeService).validateLoginCode("13800000000", "654321", true);
    }

    @Test
    @DisplayName("验证码错误时应抛出异常，不查数据库")
    void shouldRejectWhenSmsCodeInvalid() {
        doThrow(new BusinessException("短信验证码错误"))
                .when(smsCodeService).validateLoginCode("13800000000", "999999", true);

        SmsPlatformLoginHandler handler = new SmsPlatformLoginHandler(smsCodeService, platformUserMapper);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> handler.authenticate(PlatformLoginRequest.sms("13800000000", "999999"))
        );

        assertEquals("短信验证码错误", exception.getMessage());
        verify(platformUserMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("用户不存在时应抛出业务异常")
    void shouldRejectWhenUserNotFound() {
        doNothing().when(smsCodeService).validateLoginCode("13800000000", "654321", true);
        when(platformUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        SmsPlatformLoginHandler handler = new SmsPlatformLoginHandler(smsCodeService, platformUserMapper);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> handler.authenticate(PlatformLoginRequest.sms("13800000000", "654321"))
        );

        assertEquals("该手机号未注册", exception.getMessage());
    }

    @Test
    @DisplayName("用户禁用时应抛出业务异常")
    void shouldRejectWhenUserDisabled() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setPhone("13800000000");
        user.setStatus(0);
        user.setDeleted(0);

        doNothing().when(smsCodeService).validateLoginCode("13800000000", "654321", true);
        when(platformUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        SmsPlatformLoginHandler handler = new SmsPlatformLoginHandler(smsCodeService, platformUserMapper);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> handler.authenticate(PlatformLoginRequest.sms("13800000000", "654321"))
        );

        assertEquals("用户已禁用", exception.getMessage());
    }

    @Test
    @DisplayName("supports 应返回 SMS 登录类型")
    void shouldSupportSmsLoginType() {
        SmsPlatformLoginHandler handler = new SmsPlatformLoginHandler(smsCodeService, platformUserMapper);
        assertEquals(com.payment.enums.PlatformLoginTypeEnum.SMS, handler.supports());
    }
}
