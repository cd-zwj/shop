package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 营销活动规则创建参数。
 */
@Data
public class ActivityRuleCreateDTO {
    private Long activityId;
    private String ruleType;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Long productId;
    private String categoryCode;
    private String ruleConfigJson;
    private Integer priority;
}
