package com.payment.service.impl;

import com.payment.entity.PlatformUser;
import com.payment.enums.PlatformLoginTypeEnum;
import com.payment.mapper.PlatformUserMapper;
import com.payment.service.login.PlatformLoginHandler;
import com.payment.service.login.PlatformLoginRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
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

        PlatformIdentityServiceImpl service = spy(new PlatformIdentityServiceImpl(platformUserMapper, List.of(passwordLoginHandler)));
        doReturn("test-token").when(service).createLoginSession(user);

        String token = service.login(PlatformLoginRequest.password("demo-user", "secret"));

        assertEquals("test-token", token);
    }
}
