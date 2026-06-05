package com.payment.service.login.impl;

import com.payment.common.BusinessException;
import com.payment.service.login.PlatformLoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 短信平台登录测试类，用于验证短信平台登录相关逻辑。
 */
class SmsPlatformLoginHandlerTest {

    @Test
    @DisplayName("短信登录当前暂未开通，应抛出业务异常")
    void shouldRejectSmsLoginAsNotEnabled() {
        SmsPlatformLoginHandler handler = new SmsPlatformLoginHandler();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> handler.authenticate(PlatformLoginRequest.sms("13800000000", "654321"))
        );

        assertEquals("短信登录暂未开通", exception.getMessage());
    }
}
