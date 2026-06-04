package com.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮箱认证配置类，用于承载邮箱认证相关配置参数。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.auth.email")
public class EmailAuthProperties {

    private boolean enabled;

    private String from;

    private String subjectPrefix = "[SalesSystem]";

    private int codeTtlMinutes = 10;

    private int sendCooldownSeconds = 60;

    private int maxDailySendCount = 20;
}
