package com.payment.dto;

import lombok.Data;

@Data
public class CouponEffectVO {
    private Long templateId;
    private String templateName;
    private Integer totalQuantity;
    private Integer receivedCount;
    private Integer usedCount;
    private Integer remainingStock;
    private Double writeOffRate;
}
