package com.payment.service;

import com.payment.dto.ExternalPaymentQueryResult;
import com.payment.dto.PayResponseDTO;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.dto.RefundQueryResultDTO;
import com.payment.dto.RefundRequestDTO;
import com.payment.dto.RefundSubmitResultDTO;
import com.payment.entity.PaymentBill;
import com.payment.entity.RefundRecord;

/**
 * 支付渠道提供者接口（策略模式）。
 * <p>
 * 定义第三方支付渠道的统一操作契约，包括支付创建、回调验签、
 * 支付状态查询、退款提交和退款查询。
 * 各支付渠道（微信支付、支付宝、第三方支付等）实现此接口以接入统一的支付体系。
 */
public interface PaymentProvider {

    /**
     * 获取支付渠道代码。
     * <p>
     * 返回此提供者对应的渠道标识，用于路由和匹配。
     *
     * @return 渠道代码字符串（如 WECHAT、ALIPAY、EXT_PROVIDER）
     */
    String getChannelCode();

    /**
     * 创建外部支付订单。
     * <p>
     * 调用第三方支付平台统一下单接口，返回前端所需的支付参数。
     *
     * @param paymentBill 支付账单实体
     * @return 支付响应 DTO，包含预支付参数或收银台 URL
     */
    PayResponseDTO createPayment(PaymentBill paymentBill);

    /**
     * 验证支付回调的合法性（签名验证）。
     *
     * @param callbackDTO 回调数据
     * @return true-验证通过，false-验证失败
     */
    boolean verifyCallback(PaymentCallbackDTO callbackDTO);

    /**
     * 向第三方支付平台查询支付订单状态。
     *
     * @param paymentBill 支付账单实体
     * @return 外部支付查询结果，包含交易状态和支付信息
     */
    ExternalPaymentQueryResult queryPayment(PaymentBill paymentBill);

    /**
     * 判断此渠道是否支持退款功能。
     *
     * @return true-支持退款，false-不支持
     */
    boolean supportsRefund();

    /**
     * 向第三方支付平台发起退款请求。
     *
     * @param paymentBill 支付账单实体
     * @param requestDTO  退款请求参数（退款金额、原因等）
     * @return 退款提交结果，包含退款单号和提交状态
     */
    RefundSubmitResultDTO refund(PaymentBill paymentBill, RefundRequestDTO requestDTO);

    /**
     * 向第三方支付平台查询退款状态。
     *
     * @param refundRecord 退款记录实体
     * @return 退款查询结果，包含退款状态和退款金额信息
     */
    RefundQueryResultDTO queryRefund(RefundRecord refundRecord);
}
