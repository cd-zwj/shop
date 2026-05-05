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
    private ExtProvider extProvider;
    
    /**
     * 充值订单超时时间（分钟）
     */
    private Integer rechargeOrderTimeoutMinutes = 15;
    
    @Data
    public static class Wechat {
        private String appId;
        private String mchId;
        private String apiV3Key;
        private String certPath;
        private String keyPath;
        private String notifyUrl;
        /**
         * 商户API证书序列号
         */
        private String merchantSerialNumber;
        /**
         * 私钥内容（可选，如果不使用文件路径）
         */
        private String privateKey;
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

    @Data
    public static class ExtProvider {
        private String baseUrl = "https://pay.myzfw.com";
        private String merchantId;
        private String merchantKey;
        private String notifyUrl;
        private String returnUrl;
        private String defaultPayType = "alipay";
        private String defaultDevice = "pc";
        private String clientIp = "127.0.0.1";
    }
}

