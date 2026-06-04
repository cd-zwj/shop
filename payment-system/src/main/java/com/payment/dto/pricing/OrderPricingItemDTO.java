package com.payment.dto.pricing;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单定价商品项。
 */
@Data
public class OrderPricingItemDTO {
    private Long productId;
    private String category;
    private BigDecimal unitPrice;
    private Integer quantity;
}
