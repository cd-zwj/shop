package com.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class V1MerchantProductUpsertDTO {

    private String productCode;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    private String unit;

    private String category;

    private String description;

    private String imageUrl;

    @NotNull(message = "库存不能为空")
    private Integer stock;

    /**
     * active / inactive / out_of_stock
     */
    private String status;
}
