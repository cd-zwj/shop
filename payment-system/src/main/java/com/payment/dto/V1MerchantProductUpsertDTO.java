package com.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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

    private Long storeId;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    /**
     * active / inactive / out_of_stock
     */
    private String status;

    /**
     * 商品类型：PHYSICAL / VIRTUAL / CARD_KEY / SERVICE / SUBSCRIPTION
     * 不传时由 Service 兜底为 PHYSICAL。
     */
    private String productType;

    /**
     * 交付配置(JSON 字符串)，按 productType 解读，例如：
     * VIRTUAL = {"contentUrl":"...","accountInfo":"..."}
     * SUBSCRIPTION = {"validityDays":30}
     */
    private String deliveryConfig;
}
