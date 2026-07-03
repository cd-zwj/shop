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

/**
 * 支付回调签名验证器实现。
 * <p>
 * 支持三种支付渠道的验签：
 * <ul>
 *   <li>微信支付：SHA256withRSA，验证 wechatpay-signature 头部签名</li>
 *   <li>支付宝（含网页支付）：RSA2 公钥验签，校验 app_id、total_amount、seller_id</li>
 *   <li>第三方扩展渠道：HMAC-SHA256 签名验证</li>
 * </ul>
 * 被标记为 {@code @Primary}，作为 {@link PaymentSignatureVerifier} 的默认实现。
 */
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

    /**
     * 根据支付渠道编码分发验签逻辑。
     *
     * @param channelCode 支付渠道编码（WECHAT / ALIPAY / ALIPAY_PAGE / EXT_PROVIDER）
     * @param dto         回调数据，包含原始请求体
     * @param headers     请求头信息，微信和第三方渠道用于提取签名
     * @return 验签通过返回 {@code true}
     */
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

    /**
     * 验证支付宝表单回调的合法性（用于传统 form POST 回调场景）。
     * <p>
     * 依次校验：RSA2 签名 → out_trade_no 存在性 → total_amount 一致性 → seller_id 一致性。
     *
     * @param params 支付宝回调参数（键值对）
     * @return 所有校验通过返回 {@code true}
     */
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

            // 校验 total_amount（必须字段，缺值直接拒绝）
            String totalAmountStr = params.get("total_amount");
            if (totalAmountStr == null || totalAmountStr.isEmpty()) {
                log.warn("支付宝回调缺少 total_amount");
                return false;
            }
            if (bill.getPayAmount() == null) {
                log.warn("支付宝回调账单金额为空, billNo={}", outTradeNo);
                return false;
            }
            BigDecimal callbackAmount = new BigDecimal(totalAmountStr);
            BigDecimal billAmount = bill.getPayAmount().setScale(2, RoundingMode.HALF_UP);
            if (callbackAmount.compareTo(billAmount) != 0) {
                log.warn("支付宝回调金额不一致: callback={}, bill={}", callbackAmount, billAmount);
                return false;
            }

            // 校验 seller_id（必须字段，缺值直接拒绝）
            String sellerId = params.get("seller_id");
            String configSellerId = config.getAlipay().getSellerId();
            if (sellerId == null || sellerId.isEmpty()) {
                log.warn("支付宝回调缺少 seller_id");
                return false;
            }
            if (configSellerId == null || configSellerId.isEmpty()) {
                log.warn("支付宝 seller_id 未配置，无法校验回调");
                return false;
            }
            if (!sellerId.equals(configSellerId)) {
                log.warn("支付宝回调 seller_id 不一致: callback={}, config={}", sellerId, configSellerId);
                return false;
            }

            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("支付宝验签异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 判断支付宝交易状态是否为成功（TRADE_SUCCESS 或 TRADE_FINISHED）。
     */
    public boolean verifyTradeSuccess(Map<String, String> params) {
        if (params == null) {
            return false;
        }
        String tradeStatus = params.get("trade_status");
        return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
    }

    /**
     * 验证支付宝 JSON 格式回调（异步通知）的签名。
     */
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

    /**
     * 验证微信支付 V3 回调签名。
     * <p>
     * 使用微信平台证书的公钥验证 timestamp + nonce + body 组合的 SHA256withRSA 签名。
     */
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

    /**
     * 验证第三方扩展支付渠道的 HMAC-SHA256 签名。
     */
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

    /**
     * 从文件系统加载微信支付 X.509 证书。
     */
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

    /**
     * 将参数 Map 拼接为 key1=value1&key2=value2 格式的待签名字符串。
     */
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

    /** 计算 HMAC-SHA256 签名。 */
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
