package com.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMerchantWalletRechargeDTO {
    @NotNull(message = "规则ID不能为空")
    private Long ruleId;
}
