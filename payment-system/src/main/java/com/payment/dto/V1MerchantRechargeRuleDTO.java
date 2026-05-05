package com.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class V1MerchantRechargeRuleDTO {

    private Long id;

    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于 0")
    private BigDecimal rechargeAmount;

    @NotNull(message = "赠送金额不能为空")
    @DecimalMin(value = "0.00", message = "赠送金额不能小于 0")
    private BigDecimal giftAmount;

    @NotNull(message = "赠送积分不能为空")
    private Integer giftPoints;

    @NotNull(message = "状态不能为空")
    private Boolean enabled;

    private Integer sortOrder;
}
