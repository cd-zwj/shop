package com.payment.service.login.impl;

import com.payment.common.BusinessException;
import com.payment.entity.PlatformUser;
import com.payment.enums.EmailCodeSceneEnum;
import com.payment.mapper.PlatformUserMapper;
import com.payment.service.EmailCodeService;
import com.payment.service.login.PlatformLoginRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 邮箱平台登录测试类，用于验证邮箱平台登录相关逻辑。
 */
@ExtendWith(MockitoExtension.class)
class EmailPlatformLoginHandlerTest {

    @Mock
    private PlatformUserMapper platformUserMapper;

    @Mock
    private EmailCodeService emailCodeService;

    /**
     * 判断是否需要AuthenticateVerified用户按邮箱编码。
     */
    @Test
    void shouldAuthenticateVerifiedUserByEmailCode() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setEmail("demo@test.com");
        user.setEmailVerified(1);
        user.setStatus(1);
        user.setDeleted(0);
        when(platformUserMapper.selectOne(any())).thenReturn(user);

        EmailPlatformLoginHandler handler = new EmailPlatformLoginHandler(platformUserMapper, emailCodeService);
        PlatformUser result = handler.authenticate(PlatformLoginRequest.email("Demo@Test.com", "123456"));

        assertEquals(1L, result.getId());
        verify(emailCodeService).validateCode("demo@test.com", "123456", EmailCodeSceneEnum.LOGIN, true);
    }

    /**
     * 判断是否需要RejectUnverified邮箱用户。
     */
    @Test
    void shouldRejectUnverifiedEmailUser() {
        PlatformUser user = new PlatformUser();
        user.setEmail("demo@test.com");
        user.setEmailVerified(0);
        user.setStatus(1);
        user.setDeleted(0);
        when(platformUserMapper.selectOne(any())).thenReturn(user);

        EmailPlatformLoginHandler handler = new EmailPlatformLoginHandler(platformUserMapper, emailCodeService);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> handler.authenticate(PlatformLoginRequest.email("demo@test.com", "123456"))
        );

        assertEquals("邮箱未绑定账号或未完成验证", exception.getMessage());
    }
}
