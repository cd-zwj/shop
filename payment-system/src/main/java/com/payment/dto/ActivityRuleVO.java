package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 营销活动规则视图对象。
 */
@Data
public class ActivityRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
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
