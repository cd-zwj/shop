package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户端商品详情视图对象，展示商品的完整信息。
 */
@Data
public class V1MerchantProductVO {

    /** 商品 ID */
    private Long id;

    /** 所属租户（商户）ID */
    private Long tenantId;

    /** 商品编码 */
    private String productCode;

    /** 商品名称 */
    private String name;

    /** 商品单价（元） */
    private BigDecimal price;

    /** 计量单位 */
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
    private Integer stock;

    /**
     * 商品状态：active / inactive / out_of_stock
     */
    private String status;

    /** 商品类型：PHYSICAL / VIRTUAL / CARD_KEY / SERVICE / SUBSCRIPTION */
    private String productType;

    /** 交付配置(JSON 字符串)，按 productType 解读 */
    private String deliveryConfig;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
