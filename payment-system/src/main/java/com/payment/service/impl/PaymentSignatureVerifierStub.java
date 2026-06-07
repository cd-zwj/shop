package com.payment.service.impl;

import com.payment.dto.PaymentCallbackDTO;
import com.payment.service.PaymentSignatureVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

/**
 * 支付回调验签 -- 桩实现（仅开发环境可用）。
 * <p>
 * 非 dev 环境下所有验签请求均返回 false（fail-closed），
 * 阻止未经验证的回调进入业务流程。
 * <p>
 * TODO: 接入实际验签逻辑：
 * - 微信支付 v3: 使用 wechatpay-java 的 NotificationParser 验证签名
 * - 支付宝: 使用 AlipaySignature.rsaCheckV1() 验签
 * - 第三方: 验证 HMAC / RSA 签名
 * <p>
 * 生产环境部署前必须替换为真实验签实现。
 */
@Slf4j
@Service
public class PaymentSignatureVerifierStub implements PaymentSignatureVerifier {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public boolean verify(String channelCode, PaymentCallbackDTO dto, Map<String, String> headers) {
        if (!isDevProfile()) {
            log.error("[SECURITY] 支付回调验签未实现（桩模式），拒绝非开发环境请求, channel={}, billNo={}",
                    channelCode, dto.getBillNo());
            return false;
        }
        log.warn("[SECURITY] 支付回调验签未实现（桩模式，仅限开发环境），channel={}, billNo={} -- 请尽快接入真实验签",
                channelCode, dto.getBillNo());
        return true;
    }

    @Override
    public boolean verifyAlipayCallback(Map<String, String> params) {
        if (!isDevProfile()) {
            log.error("[SECURITY] 支付宝回调验签未实现（桩模式），拒绝非开发环境请求");
            return false;
        }
        log.warn("[SECURITY] 支付宝回调验签未实现（桩模式，仅限开发环境）-- 请尽快接入真实验签");
        return true;
    }

    private boolean isDevProfile() {
        return Arrays.asList(activeProfile.split(",")).contains("dev");
    }
}
