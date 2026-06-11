package com.payment.service.sms.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * MockSmsSender 测试类。
 */
class MockSmsSenderTest {

    @Test
    @DisplayName("MockSmsSender send 方法不抛异常且能正常执行")
    void shouldSendWithoutThrowing() {
        MockSmsSender sender = new MockSmsSender();
        assertDoesNotThrow(() -> sender.send("13800000000", "123456"));
    }

    @Test
    @DisplayName("MockSmsSender 能处理各种手机号格式")
    void shouldHandleVariousPhoneFormats() {
        MockSmsSender sender = new MockSmsSender();
        assertDoesNotThrow(() -> {
            sender.send("13800000000", "000000");
            sender.send("+8613800000000", "999999");
            sender.send("15012345678", "111111");
        });
    }
}
