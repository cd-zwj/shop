package com.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商户端积分规则创建/更新请求参数。
 */
@Data
public class V1MerchantPointsRuleDTO {

    /** 积分比例（每消费 1 元获得的积分数） */
    @NotNull(message = "积分比例不能为空")
    @Min(value = 0, message = "积分比例不能小于 0")
    private Integer pointsRatio;

    /** 是否启用积分规则 */
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
