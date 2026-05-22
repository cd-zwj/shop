package com.payment.service.login.impl;

import com.payment.common.BusinessException;
import com.payment.entity.PlatformUser;
import com.payment.mapper.PlatformUserMapper;
import com.payment.service.login.PlatformLoginRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordPlatformLoginHandlerTest {

    @Mock
    private PlatformUserMapper platformUserMapper;

    @Test
    void shouldAuthenticateActiveUserByPassword() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setUsername("alice");
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setStatus(1);
        user.setDeleted(0);

        when(platformUserMapper.selectOne(any())).thenReturn(user);

        PasswordPlatformLoginHandler handler = new PasswordPlatformLoginHandler(platformUserMapper);

        PlatformUser result = handler.authenticate(PlatformLoginRequest.password("alice", "123456"));

        assertEquals(1L, result.getId());
    }

    @Test
    void shouldRejectWrongPassword() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        PlatformUser user = new PlatformUser();
        user.setUsername("alice");
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setStatus(1);
        user.setDeleted(0);

        when(platformUserMapper.selectOne(any())).thenReturn(user);

        PasswordPlatformLoginHandler handler = new PasswordPlatformLoginHandler(platformUserMapper);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> handler.authenticate(PlatformLoginRequest.password("alice", "wrong-password"))
        );

        assertEquals("用户名或密码错误", exception.getMessage());
    }
}
