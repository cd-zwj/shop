package com.payment.dto;

import lombok.Data;

@Data
public class V1MerchantCardKeySummaryVO {
    private Long productId;
    private Integer availableCount;
    private Integer usedCount;
    private Integer returnedCount;
    private Integer disabledCount;
    private Integer totalCount;
}
