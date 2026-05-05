package com.payment.dto;

import com.payment.enums.PaymentChannelCodeEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMerchantWalletRechargeDTO {
    @NotNull(message = "Rule id is required")
    private Long ruleId;

    @NotNull(message = "Payment channel is required")
    private PaymentChannelCodeEnum paymentChannelCode;
}
