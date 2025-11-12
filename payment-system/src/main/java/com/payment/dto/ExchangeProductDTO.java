package com.payment.dto;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 积分兑换商品DTO
 */
@Data
public class ExchangeProductDTO {
    
    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;
    
    /**
     * 所需积分
     */
    @NotNull(message = "所需积分不能为空")
    @Min(value = 1, message = "所需积分必须大于0")
    private Integer pointsRequired;
    
    /**
     * 兑换库存
     */
    @NotNull(message = "兑换库存不能为空")
    @Min(value = 0, message = "兑换库存不能小于0")
    private Integer stock;
    
    /**
     * 状态（0-下架，1-上架）
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
