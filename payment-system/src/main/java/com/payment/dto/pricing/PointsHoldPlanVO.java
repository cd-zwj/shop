package com.payment.dto.pricing;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 积分预占计划视图对象，用于定价引擎输出积分抵扣的预占方案。
 */
@Data
public class PointsHoldPlanVO {
    /** 是否需要预占积分 */
    private Boolean needHold;
    /** 预占积分数 */
    private Integer holdPoints;
    /** 积分抵扣金额 */
    private BigDecimal deductAmount;
    /** 预占状态（如 SUCCESS、INSUFFICIENT） */
    private String status;
}
