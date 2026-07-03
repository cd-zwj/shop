package com.payment.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 支付配置属性类。
 * <p>
 * 绑定 {@code payment.*} 前缀配置，包含微信支付、支付宝、第三方支付提供商的连接参数。
 * 启动时通过 {@link #validate()} 校验支付宝必填字段，防止运行时才发现缺失配置。
 * </p>
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "payment")
public class PaymentConfig {

    /** 微信支付配置 */
    private Wechat wechat;
    /** 支付宝支付配置 */
    private Alipay alipay;
    /** 第三方支付提供商配置 */
    private ExtProvider extProvider;

    /**
     * 充值订单超时时间（分钟）
     */
    private Integer rechargeOrderTimeoutMinutes = 15;

    /**
     * 启动时校验支付配置完整性。
     *
     * @throws IllegalStateException 支付宝必填字段缺失时抛出
     */
    @PostConstruct
    public void validate() {
        if (alipay != null) {
            if (!StringUtils.hasText(alipay.getSellerId())) {
                throw new IllegalStateException("payment.alipay.seller-id 未配置，支付宝支付无法使用");
            }
            if (!StringUtils.hasText(alipay.getAppId())) {
                throw new IllegalStateException("payment.alipay.app-id 未配置");
            }
        }
    }
    
    /**
     * 微信支付 V3 配置内部类。
     */
    @Data
    public static class Wechat {
        /** 微信应用 ID */
        private String appId;
        /** 商户号 */
        private String mchId;
        /** API v3 密钥 */
        private String apiV3Key;
        /** 证书文件路径 */
        private String certPath;
        /** 私钥文件路径 */
        private String keyPath;
        /** 支付回调通知地址 */
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

    /**
     * 支付宝配置内部类。
     */
    @Data
    public static class Alipay {
        /** 应用 ID */
        private String appId;
        /** 应用私钥 */
        private String privateKey;
        /** 支付宝公钥 */
        private String publicKey;
        /** 支付宝网关地址 */
        private String gatewayUrl;
        /** 异步通知回调地址 */
        private String notifyUrl;
        /** 同步跳转地址 */
        private String returnUrl;
        /** 卖家支付宝用户 ID */
        private String sellerId;
    }

    /**
     * 第三方支付提供商配置内部类。
     */
    @Data
    public static class ExtProvider {
        /** 支付网关基础地址 */
        private String baseUrl = "https://pay.myzfw.com";
        /** 商户号 */
        private String merchantId;
        /** 商户密钥 */
        private String merchantKey;
        /** 异步通知回调地址 */
        private String notifyUrl;
        /** 同步跳转地址 */
        private String returnUrl;
        /** 默认支付方式，默认支付宝 */
        private String defaultPayType = "alipay";
        /** 默认设备类型，默认 PC 端 */
        private String defaultDevice = "pc";
        /** 客户端 IP */
        private String clientIp = "127.0.0.1";
    }
}

