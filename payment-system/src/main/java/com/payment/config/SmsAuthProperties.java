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

    /** 是否启用短信认证功能 */
    private boolean enabled;

    /** 短信服务提供商，默认 "mock"（开发环境模拟） */
    private String provider = "mock";

    /** 短信签名 */
    private String signName;

    /** 短信模板编码 */
    private String templateCode;

    /** 验证码有效时长（分钟），默认 10 分钟 */
    private int codeTtlMinutes = 10;

    /** 发送冷却时间（秒），默认 60 秒，防止频繁发送 */
    private int sendCooldownSeconds = 60;

    /** 每日最大发送次数，默认 20 次 */
    private int maxDailySendCount = 20;
}
