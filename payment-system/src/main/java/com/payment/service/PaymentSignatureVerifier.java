package com.payment.service;

import com.payment.dto.PaymentCallbackDTO;

import java.util.Map;

/**
 * 支付回调签名验证服务。
 * <p>
 * 不同支付渠道（微信、支付宝、第三方）使用各自的验签逻辑，
 * 本接口提供统一的验证入口。
 */
public interface PaymentSignatureVerifier {

    /**
     * 验证支付回调签名（JSON body 类型，如微信支付 v3）。
     *
     * @param channelCode 支付渠道代码（WECHAT / ALIPAY / EXT_PROVIDER）
     * @param dto         回调数据
     * @param headers     HTTP 请求头（含签名相关 header）
     * @return true 验签通过，false 验签失败
     */
    boolean verify(String channelCode, PaymentCallbackDTO dto, Map<String, String> headers);

    /**
     * 验证支付宝页面回调签名（form 表单类型）。
     *
     * @param params 回调参数
     * @return true 验签通过，false 验签失败
     */
    boolean verifyAlipayCallback(Map<String, String> params);
}
