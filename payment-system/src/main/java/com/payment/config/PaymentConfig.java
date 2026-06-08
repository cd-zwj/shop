package com.payment.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付配置
 */
@Slf4j
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

    @PostConstruct
    public void validate() {
        if (alipay != null) {
            if (alipay.getSellerId() == null || alipay.getSellerId().isEmpty()) {
                log.warn("⚠️ payment.alipay.seller-id 未配置，支付宝回调 seller_id 校验将拒绝所有回调");
            }
            if (alipay.getAppId() == null || alipay.getAppId().isEmpty()) {
                log.warn("⚠️ payment.alipay.app-id 未配置");
            }
        }
    }
    
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
        private String sellerId;
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

