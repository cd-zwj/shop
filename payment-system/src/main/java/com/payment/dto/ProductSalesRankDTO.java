package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品销售排行DTO
 */
@Data
public class ProductSalesRankDTO {
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 商品编码
     */
    private String productCode;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 商品图片
     */
    private String productImage;
    
    /**
     * 销售数量
     */
    private Integer salesQuantity;
    
    /**
     * 销售额
     */
    private BigDecimal salesAmount;
}
