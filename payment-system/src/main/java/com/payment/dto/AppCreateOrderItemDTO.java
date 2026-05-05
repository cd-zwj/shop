package com.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户端下单商品项。
 */
@Data
public class AppCreateOrderItemDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;
}
