package com.payment.dto;

import lombok.Data;

@Data
public class MarketingEffectSummaryVO {
    private Integer templateCount;
    private Integer activeTemplateCount;
    private Integer receivedCount;
    private Integer usedCount;
    private Integer remainingStock;
    private Double writeOffRate;
}
