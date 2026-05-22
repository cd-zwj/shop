package com.payment.service.login.impl;

import com.payment.common.BusinessException;
import com.payment.entity.PlatformUser;
import com.payment.entity.PlatformUserAuth;
import com.payment.mapper.PlatformUserAuthMapper;
import com.payment.mapper.PlatformUserMapper;
import com.payment.service.login.PlatformLoginRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThirdPartyPlatformLoginHandlerTest {

    @Mock
    private PlatformUserAuthMapper platformUserAuthMapper;

    @Mock
    private PlatformUserMapper platformUserMapper;

    @Test
    void shouldAuthenticateBoundThirdPartyUser() {
        PlatformUserAuth auth = new PlatformUserAuth();
        auth.setPlatformUserId(7L);
        auth.setAuthType("GITHUB");
        auth.setAuthKey("github-user-7");

        PlatformUser user = new PlatformUser();
        user.setId(7L);
        user.setUsername("octocat");
        user.setStatus(1);
        user.setDeleted(0);

        when(platformUserAuthMapper.selectOne(any())).thenReturn(auth);
        when(platformUserMapper.selectById(7L)).thenReturn(user);

        ThirdPartyPlatformLoginHandler handler = new ThirdPartyPlatformLoginHandler(platformUserAuthMapper, platformUserMapper);

        PlatformUser result = handler.authenticate(PlatformLoginRequest.thirdParty("GITHUB", "github-user-7"));

        assertEquals(7L, result.getId());
    }

    @Test
    void shouldRejectWhenThirdPartyBindingDoesNotExist() {
        when(platformUserAuthMapper.selectOne(any())).thenReturn(null);

        ThirdPartyPlatformLoginHandler handler = new ThirdPartyPlatformLoginHandler(platformUserAuthMapper, platformUserMapper);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> handler.authenticate(PlatformLoginRequest.thirdParty("WECHAT", "openid-1"))
        );

        assertEquals("第三方账号未绑定平台用户", exception.getMessage());
    }
}
