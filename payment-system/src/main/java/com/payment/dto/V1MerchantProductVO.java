package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class V1MerchantProductVO {

    private Long id;

    private Long tenantId;

    private String productCode;

    private String name;

    private BigDecimal price;

    private String unit;

    private String category;

    private String description;

    private String imageUrl;

    private Long storeId;

    private Integer stock;

    /**
     * active / inactive / out_of_stock
     */
    private String status;

    /** 商品类型：PHYSICAL / VIRTUAL / CARD_KEY / SERVICE / SUBSCRIPTION */
    private String productType;

    /** 交付配置(JSON 字符串)，按 productType 解读 */
    private String deliveryConfig;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
