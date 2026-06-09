package com.payment.dto;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 积分兑换商品DTO
 */
@Data
public class ExchangeProductDTO {

    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
    private String productName;

    /**
     * 商品图片
     */
    private String productImage;

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
     * 兑换限制（每人）
     */
    private Integer exchangeLimit;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 状态（0-下架，1-上架）
     */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 排序
     */
    private Integer sortOrder;
}
