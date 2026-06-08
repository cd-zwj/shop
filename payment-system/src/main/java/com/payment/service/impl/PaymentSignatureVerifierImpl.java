package com.payment.service.impl;

import com.alipay.api.internal.util.AlipaySignature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.config.PaymentConfig;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.entity.PaymentBill;
import com.payment.mapper.PaymentBillMapper;
import com.payment.service.PaymentSignatureVerifier;
import com.payment.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Primary
@Service
public class PaymentSignatureVerifierImpl implements PaymentSignatureVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final PaymentConfig config;
    private final PaymentBillMapper paymentBillMapper;

    public PaymentSignatureVerifierImpl(PaymentConfig config, PaymentBillMapper paymentBillMapper) {
        this.config = config;
        this.paymentBillMapper = paymentBillMapper;
    }

    @Override
    public boolean verify(String channelCode, PaymentCallbackDTO dto, Map<String, String> headers) {
        if (channelCode == null) {
            log.warn("验签失败: channelCode 为空");
            return false;
        }

        return switch (channelCode) {
            case "WECHAT" -> verifyWechat(dto, headers);
            case "ALIPAY", "ALIPAY_PAGE" -> verifyAlipayJsonCallback(dto, headers);
            case "EXT_PROVIDER" -> verifyExtProvider(dto, headers);
            default -> {
                log.warn("验签失败: 未知渠道 {}", channelCode);
                yield false;
            }
        };
    }

    @Override
    public boolean verifyAlipayCallback(Map<String, String> params) {
        try {
            if (params == null || !params.containsKey("sign")) {
                log.warn("支付宝验签失败: 缺少 sign 参数");
                return false;
            }

            String appId = config.getAlipay().getAppId();
            if (appId == null || !appId.equals(params.get("app_id"))) {
                log.warn("支付宝验签失败: app_id 不匹配");
                return false;
            }

            String signType = params.getOrDefault("sign_type", "RSA2");
            Map<String, String> sorted = new TreeMap<>(params);
            sorted.remove("sign");
            sorted.remove("sign_type");

            String content = buildSignContent(sorted);
            String publicKey = config.getAlipay().getPublicKey();

            boolean valid = AlipaySignature.rsaCheckContent(content, params.get("sign"), publicKey, signType);
            if (!valid) {
                log.warn("支付宝验签失败: 签名不匹配");
                return false;
            }

            // 校验 out_trade_no 是否存在于系统中
            String outTradeNo = params.get("out_trade_no");
            if (outTradeNo == null || outTradeNo.isBlank()) {
                log.warn("支付宝回调缺少 out_trade_no");
                return false;
            }
            PaymentBill bill = paymentBillMapper.selectOne(
                    new LambdaQueryWrapper<PaymentBill>().eq(PaymentBill::getBillNo, outTradeNo));
            if (bill == null) {
                log.warn("支付宝回调 out_trade_no 不存在: {}", outTradeNo);
                return false;
            }

            // 校验 total_amount 是否与账单金额一致
            String totalAmountStr = params.get("total_amount");
            if (totalAmountStr != null && bill.getPayAmount() != null) {
                BigDecimal callbackAmount = new BigDecimal(totalAmountStr);
                BigDecimal billAmount = bill.getPayAmount().setScale(2, RoundingMode.HALF_UP);
                if (callbackAmount.compareTo(billAmount) != 0) {
                    log.warn("支付宝回调金额不一致: callback={}, bill={}", callbackAmount, billAmount);
                    return false;
                }
            }

            // 校验 seller_id（收款方）
            String sellerId = params.get("seller_id");
            if (sellerId != null && config.getAlipay().getSellerId() != null) {
                if (!sellerId.equals(config.getAlipay().getSellerId())) {
                    log.warn("支付宝回调 seller_id 不一致: callback={}, config={}", sellerId, config.getAlipay().getSellerId());
                    return false;
                }
            }

            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("支付宝验签异常: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean verifyTradeSuccess(Map<String, String> params) {
        if (params == null) {
            return false;
        }
        String tradeStatus = params.get("trade_status");
        return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
    }

    private boolean verifyAlipayJsonCallback(PaymentCallbackDTO dto, Map<String, String> headers) {
        try {
            if (dto == null) {
                log.warn("支付宝验签失败: DTO 为空");
                return false;
            }
            if (dto.getRawBody() == null || dto.getRawBody().isBlank()) {
                log.warn("支付宝验签失败: 缺少原始报文");
                return false;
            }

            String sign = headers.getOrDefault("sign", headers.get("signature"));
            if (sign == null || sign.isBlank()) {
                log.warn("支付宝验签失败: 缺少签名");
                return false;
            }

            Map<String, String> contentMap = JsonUtils.fromJson(dto.getRawBody(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
            if (!contentMap.containsKey("app_id")) {
                contentMap.put("app_id", config.getAlipay().getAppId());
            }

            Map<String, String> sorted = new TreeMap<>(contentMap);
            String content = buildSignContent(sorted);
            String publicKey = config.getAlipay().getPublicKey();
            String signType = headers.getOrDefault("sign_type", "RSA2");

            boolean valid = AlipaySignature.rsaCheckContent(content, sign, publicKey, signType);
            if (!valid) {
                log.warn("支付宝验签失败: 签名不匹配");
            }
            return valid;
        } catch (Exception e) {
            log.warn("支付宝验签异常: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean verifyWechat(PaymentCallbackDTO dto, Map<String, String> headers) {
        if (dto == null || dto.getRawBody() == null || dto.getRawBody().isBlank()) {
            log.warn("微信验签失败: 缺少原始报文");
            return false;
        }

        String timestamp = headers.get("wechatpay-timestamp");
        String nonce = headers.get("wechatpay-nonce");
        String signature = headers.get("wechatpay-signature");
        String serial = headers.get("wechatpay-serial");

        if (timestamp == null || nonce == null || signature == null) {
            log.warn("微信验签失败: 缺少验签头信息");
            return false;
        }

        String message = timestamp + "\n" + nonce + "\n" + dto.getRawBody() + "\n";

        try {
            java.security.cert.X509Certificate certificate = loadWechatCertificate(serial);
            java.security.Signature sign = java.security.Signature.getInstance("SHA256withRSA");
            sign.initVerify(certificate.getPublicKey());
            sign.update(message.getBytes(StandardCharsets.UTF_8));
            boolean valid = sign.verify(Base64.getDecoder().decode(signature));
            if (!valid) {
                log.warn("微信验签失败: 签名不匹配");
            }
            return valid;
        } catch (Exception e) {
            log.warn("微信验签异常: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean verifyExtProvider(PaymentCallbackDTO dto, Map<String, String> headers) {
        if (dto == null || dto.getRawBody() == null || dto.getRawBody().isBlank()) {
            log.warn("第三方验签失败: 缺少原始报文");
            return false;
        }

        String merchantKey = config.getExtProvider().getMerchantKey();
        if (merchantKey == null || merchantKey.isBlank()) {
            log.warn("第三方验签失败: 缺少商户密钥配置");
            return false;
        }

        String sign = headers.getOrDefault("sign", headers.get("signature"));
        if (sign == null || sign.isBlank()) {
            log.warn("第三方验签失败: 缺少签名");
            return false;
        }

        try {
            Map<String, String> payload = JsonUtils.fromJson(dto.getRawBody(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
            Map<String, String> sorted = new TreeMap<>(payload);
            String content = buildSignContent(sorted);
            String computedSign = Base64.getEncoder().encodeToString(hmacSha256(content.getBytes(StandardCharsets.UTF_8), merchantKey.getBytes(StandardCharsets.UTF_8)));

            boolean valid = computedSign.equals(sign);
            if (!valid) {
                log.warn("第三方验签失败: 签名不匹配");
            }
            return valid;
        } catch (Exception e) {
            log.warn("第三方验签异常: {}", e.getMessage(), e);
            return false;
        }
    }

    private java.security.cert.X509Certificate loadWechatCertificate(String serial) throws Exception {
        String certPath = config.getWechat().getKeyPath();
        if (certPath == null || certPath.isBlank()) {
            throw new BusinessException("微信支付证书路径未配置");
        }
        try (java.io.InputStream is = new java.io.FileInputStream(certPath)) {
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            return (java.security.cert.X509Certificate) cf.generateCertificate(is);
        }
    }

    private String buildSignContent(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (!first) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }

    private byte[] hmacSha256(byte[] data, byte[] key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key, HMAC_SHA256));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new BusinessException("HMAC 签名计算失败");
        }
    }
}
