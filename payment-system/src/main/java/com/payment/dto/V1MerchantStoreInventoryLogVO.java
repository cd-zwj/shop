package com.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 门店库存流水视图。
 */
@Data
public class V1MerchantStoreInventoryLogVO {
    private Long id;
    private Long storeId;
    private Long productId;
    private String changeType;
    private Integer changeQuantity;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private Integer lockedBefore;
    private Integer lockedAfter;
    private String bizType;
    private String bizNo;
    private Long operatorId;
    private String remark;
    private LocalDateTime createTime;
}
