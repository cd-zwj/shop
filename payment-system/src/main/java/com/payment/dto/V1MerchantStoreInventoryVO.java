package com.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 门店商品库存视图。
 */
@Data
public class V1MerchantStoreInventoryVO {
    private Long id;
    private Long tenantId;
    private Long storeId;
    private String storeName;
    private Long productId;
    private String productCode;
    private String productName;
    private Integer quantity;
    private Integer lockedQuantity;
    private Integer availableQuantity;
    private LocalDateTime updateTime;
}
