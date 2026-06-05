package com.payment.service.impl;

import com.payment.dto.PaymentCallbackDTO;
import com.payment.service.PaymentSignatureVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 支付回调验签 -- 桩实现。
 * <p>
 * TODO: 接入实际验签逻辑：
 * - 微信支付 v3: 使用 wechatpay-java 的 NotificationParser 验证签名
 * - 支付宝: 使用 AlipaySignature.rsaCheckV1() 验签
 * - 第三方: 验证 HMAC / RSA 签名
 * <p>
 * 当前为开发阶段桩实现：打印告警日志，允许所有回调通过。
 * 生产环境部署前必须替换为真实验签逻辑。
 */
@Slf4j
@Service
public class PaymentSignatureVerifierStub implements PaymentSignatureVerifier {

    @Override
    public boolean verify(String channelCode, PaymentCallbackDTO dto, Map<String, String> headers) {
        // TODO: 生产环境必须替换为真实验签
        log.warn("[SECURITY] 支付回调验签未实现（桩模式），channel={}, billNo={} -- 请尽快接入真实验签",
                channelCode, dto.getBillNo());
        return true;
    }

    @Override
    public boolean verifyAlipayCallback(Map<String, String> params) {
        // TODO: 生产环境必须替换为真实验签
        log.warn("[SECURITY] 支付宝回调验签未实现（桩模式），params={} -- 请尽快接入真实验签", params);
        return true;
    }
}
