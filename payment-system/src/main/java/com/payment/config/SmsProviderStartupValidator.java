package com.payment.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class SmsProviderStartupValidator {

    private final Environment environment;

    @PostConstruct
    public void validate() {
        String provider = environment.getProperty("app.auth.sms.provider", "mock");
        boolean prod = Arrays.stream(environment.getActiveProfiles())
                .anyMatch("prod"::equalsIgnoreCase);
        if (prod && "mock".equalsIgnoreCase(provider)) {
            throw new IllegalStateException("SMS_PROVIDER=mock is not allowed when spring profile contains prod");
        }
    }
}
