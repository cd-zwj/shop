package com.payment.dto;

import com.payment.enums.PaymentChannelCodeEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateUnifiedWalletRechargeDTO {
    @NotNull(message = "Recharge amount is required")
    @DecimalMin(value = "0.01", message = "Recharge amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Payment channel is required")
    private PaymentChannelCodeEnum paymentChannelCode;
}
