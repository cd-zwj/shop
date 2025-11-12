package com.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "payment")
public class PaymentConfig {
    
    private Wechat wechat;
    private Alipay alipay;
    
    @Data
    public static class Wechat {
        private String appId;
        private String mchId;
        private String apiV3Key;
        private String certPath;
        private String keyPath;
        private String notifyUrl;
    }
    
    @Data
    public static class Alipay {
        private String appId;
        private String privateKey;
        private String publicKey;
        private String gatewayUrl;
        private String notifyUrl;
        private String returnUrl;
    }
}

