package com.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信认证配置类，用于承载短信认证相关配置参数。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.auth.sms")
public class SmsAuthProperties {

    private boolean enabled;

    private String provider = "mock";

    private String signName;

    private String templateCode;

    private int codeTtlMinutes = 10;

    private int sendCooldownSeconds = 60;

    private int maxDailySendCount = 20;
}
