package com.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class V1MerchantPointsRuleDTO {

    @NotNull(message = "积分比例不能为空")
    @Min(value = 0, message = "积分比例不能小于 0")
    private Integer pointsRatio;

    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
