package com.payment.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmsProviderStartupValidatorTest {

    @Test
    void prodProfileShouldRejectMockSmsProvider() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.profiles.active", "prod")
                .withProperty("app.auth.sms.provider", "mock");

        SmsProviderStartupValidator validator = new SmsProviderStartupValidator(environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SMS_PROVIDER=mock");
    }

    @Test
    void devProfileShouldAllowMockSmsProvider() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.profiles.active", "dev")
                .withProperty("app.auth.sms.provider", "mock");

        SmsProviderStartupValidator validator = new SmsProviderStartupValidator(environment);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }
}
