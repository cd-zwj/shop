package com.payment.dto.pricing;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 积分预占计划。
 */
@Data
public class PointsHoldPlanVO {
    private Boolean needHold;
    private Integer holdPoints;
    private BigDecimal deductAmount;
    private String status;
}
