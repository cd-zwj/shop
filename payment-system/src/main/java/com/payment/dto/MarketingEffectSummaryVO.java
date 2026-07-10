package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MarketingEffectSummaryVO {
    private Integer templateCount;
    private Integer activeTemplateCount;
    private Integer receivedCount;
    private Integer usedCount;
    private Integer remainingStock;
    private Double writeOffRate;
    private Integer activityCount;
    private Integer activeActivityCount;
    private BigDecimal activityDiscountAmount;
}
