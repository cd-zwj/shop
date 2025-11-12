package com.payment.dto;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 积分规则DTO
 */
@Data
public class PointsRuleDTO {
    
    /**
     * 积分比例（每消费1元获得的积分）
     */
    @NotNull(message = "积分比例不能为空")
    @Min(value = 0, message = "积分比例不能小于0")
    private Integer pointsRatio;
    
    /**
     * 是否启用（0-否，1-是）
     */
    @NotNull(message = "启用状态不能为空")
    private Integer enabled;
}
