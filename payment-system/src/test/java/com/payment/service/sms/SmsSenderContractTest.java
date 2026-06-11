package com.payment.service.sms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SmsSender 接口契约测试（仅为编译检查，实际验证在实现类测试中）。
 */
class SmsSenderContractTest {

    @Test
    @DisplayName("SmsSender 接口可正常编译引用")
    void shouldCompileWithoutError() {
        SmsSender sender = (phone, code) -> {};
        assertDoesNotThrow(() -> sender.send("13800000000", "123456"));
    }
}
