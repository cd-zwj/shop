package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.common.ResultCode;
import com.payment.dto.PlatformRegisterDTO;
import com.payment.entity.PlatformUser;
import com.payment.enums.PlatformLoginTypeEnum;
import com.payment.mapper.PlatformUserMapper;
import com.payment.service.login.PlatformLoginHandler;
import com.payment.service.login.PlatformLoginRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformIdentityServiceImplTest {

    @Mock
    private PlatformUserMapper platformUserMapper;

    @Mock
    private PlatformLoginHandler passwordLoginHandler;

    @Test
    void shouldDispatchToMatchedHandlerAndReturnIssuedToken() {
        PlatformUser user = new PlatformUser();
        user.setId(12L);
        user.setUsername("demo-user");

        when(passwordLoginHandler.supports()).thenReturn(PlatformLoginTypeEnum.PASSWORD);
        when(passwordLoginHandler.authenticate(any(PlatformLoginRequest.class))).thenReturn(user);

        PlatformIdentityServiceImpl service = spy(new PlatformIdentityServiceImpl(platformUserMapper, List.of(passwordLoginHandler), new BCryptPasswordEncoder()));
        doReturn("test-token").when(service).createPlatformSession(user);

        String token = service.login(PlatformLoginRequest.password("demo-user", "secret"));

        assertEquals("test-token", token);
    }

    @Test
    void registerShouldRejectDuplicatePhoneBeforeInsert() {
        when(passwordLoginHandler.supports()).thenReturn(PlatformLoginTypeEnum.PASSWORD);
        when(platformUserMapper.selectOne(any())).thenReturn(null, existingUser());
        PlatformIdentityServiceImpl service = new PlatformIdentityServiceImpl(platformUserMapper, List.of(passwordLoginHandler), new BCryptPasswordEncoder());

        assertThatThrownBy(() -> service.register(registerDto("new-user", "13544257898", "new@example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("手机号已注册")
                .extracting("code")
                .isEqualTo(ResultCode.USER_ALREADY_EXISTS.getCode());

        verify(platformUserMapper, never()).insert(any(PlatformUser.class));
    }

    @Test
    void registerShouldRejectDuplicateEmailBeforeInsert() {
        when(passwordLoginHandler.supports()).thenReturn(PlatformLoginTypeEnum.PASSWORD);
        when(platformUserMapper.selectOne(any())).thenReturn(null, null, existingUser());
        PlatformIdentityServiceImpl service = new PlatformIdentityServiceImpl(platformUserMapper, List.of(passwordLoginHandler), new BCryptPasswordEncoder());

        assertThatThrownBy(() -> service.register(registerDto("new-user", "13544257898", "used@example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱已注册")
                .extracting("code")
                .isEqualTo(ResultCode.USER_ALREADY_EXISTS.getCode());

        verify(platformUserMapper, never()).insert(any(PlatformUser.class));
    }

    @Test
    void registerShouldTranslateDatabaseDuplicatePhoneToBusinessException() {
        when(passwordLoginHandler.supports()).thenReturn(PlatformLoginTypeEnum.PASSWORD);
        when(platformUserMapper.selectOne(any())).thenReturn(null, null, null);
        when(platformUserMapper.insert(any(PlatformUser.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry '13544257898' for key 'platform_user.uk_phone'"));
        PlatformIdentityServiceImpl service = new PlatformIdentityServiceImpl(platformUserMapper, List.of(passwordLoginHandler), new BCryptPasswordEncoder());

        assertThatThrownBy(() -> service.register(registerDto("new-user", "13544257898", "new@example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("手机号已注册")
                .extracting("code")
                .isEqualTo(ResultCode.USER_ALREADY_EXISTS.getCode());
    }

    private PlatformRegisterDTO registerDto(String username, String phone, String email) {
        PlatformRegisterDTO dto = new PlatformRegisterDTO();
        dto.setUsername(username);
        dto.setPassword("secret123");
        dto.setPhone(phone);
        dto.setEmail(email);
        return dto;
    }

    private PlatformUser existingUser() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setUsername("existing");
        return user;
    }
}
