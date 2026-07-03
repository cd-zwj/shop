package com.payment.dto;

import com.payment.enums.PaymentChannelCodeEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建统一钱包充值订单数据传输对象，用于用户自定义金额充值统一钱包。
 */
@Data
public class CreateUnifiedWalletRechargeDTO {
    /** 充值金额 */
    @NotNull(message = "Recharge amount is required")
    @DecimalMin(value = "0.01", message = "Recharge amount must be greater than 0")
    private BigDecimal amount;

    /** 支付渠道（如 WECHAT_PAY, ALIPAY） */
    @NotNull(message = "Payment channel is required")
    private PaymentChannelCodeEnum paymentChannelCode;
}
