package com.payment.service.sms.impl;

import com.payment.service.sms.SmsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock 短信发送器，用于开发/测试环境。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.auth.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsSender implements SmsSender {

    @Override
    public void send(String phone, String code) {
        log.info("Mock SMS sent to {}: code={}", phone, code);
    }
}
