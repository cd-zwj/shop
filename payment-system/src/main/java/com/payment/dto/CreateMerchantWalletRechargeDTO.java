package com.payment.dto;

import com.payment.enums.PaymentChannelCodeEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建商户钱包充值订单数据传输对象，用于商户发起钱包充值请求。
 */
@Data
public class CreateMerchantWalletRechargeDTO {
    /** 充值规则 ID */
    @NotNull(message = "Rule id is required")
    private Long ruleId;

    /** 支付渠道（如 WECHAT_PAY, ALIPAY） */
    @NotNull(message = "Payment channel is required")
    private PaymentChannelCodeEnum paymentChannelCode;
}
