package com.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商户端商品创建/更新请求参数。
 */
@Data
public class V1MerchantProductUpsertDTO {

    /** 商品编码（不传则自动生成） */
    private String productCode;

    /** 商品名称 */
    @NotBlank(message = "商品名称不能为空")
    private String name;

    /** 商品单价（元） */
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    /** 计量单位（件、个、次等） */
    private String unit;

    /** 商品分类 */
    private String category;

    /** 商品描述 */
    private String description;

    /** 商品图片 URL */
    private String imageUrl;

    /** 所属门店 ID */
    private Long storeId;

    /** 履约形态：ONLINE_VIRTUAL / OFFLINE_SERVICE / EXPRESS_DELIVERY */
    private String fulfillmentMode;

    /** 虚拟商品类型 ID */
    private Long virtualTypeId;

    /** 虚拟商品分类 ID */
    private Long virtualCategoryId;

    /** 库存数量 */
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    /**
     * 商品状态：active / inactive / out_of_stock
     */
    private String status;

    /**
     * 商品类型：PHYSICAL / VIRTUAL / CARD_KEY / SERVICE / SUBSCRIPTION
     * 不传时由服务层兜底为 PHYSICAL。
     */
    private String productType;

    /**
     * 交付配置(JSON 字符串)，按 productType 解读，例如：
     * VIRTUAL = {"contentUrl":"...","accountInfo":"..."}
     * SUBSCRIPTION = {"validityDays":30}
     */
    private String deliveryConfig;
}
