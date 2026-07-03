package com.payment.dto.pricing;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单定价商品项数据传输对象，作为定价请求中的单个商品输入。
 */
@Data
public class OrderPricingItemDTO {
    /** 商品 ID */
    private Long productId;
    /** 商品分类（用于优惠券/活动的分类匹配） */
    private String category;
    /** 商品单价 */
    private BigDecimal unitPrice;
    /** 购买数量 */
    private Integer quantity;
}
