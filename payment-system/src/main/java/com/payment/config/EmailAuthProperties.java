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

    /** 是否启用邮箱认证功能 */
    private boolean enabled;

    /** 发件人邮箱地址 */
    private String from;

    /** 邮件主题前缀，默认 "[SalesSystem]" */
    private String subjectPrefix = "[SalesSystem]";

    /** 验证码有效时长（分钟），默认 10 分钟 */
    private int codeTtlMinutes = 10;

    /** 发送冷却时间（秒），默认 60 秒，防止频繁发送 */
    private int sendCooldownSeconds = 60;

    /** 每日最大发送次数，默认 20 次 */
    private int maxDailySendCount = 20;
}
