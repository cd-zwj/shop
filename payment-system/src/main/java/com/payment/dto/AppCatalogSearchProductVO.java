package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户端统一商品搜索卡片数据。
 */
@Data
public class AppCatalogSearchProductVO {

    private String type = "product";

    private Long id;

    private Long productId;

    private Long tenantId;

    private String tenantName;

    private String title;

    private String name;

    private String subtitle;

    private String category;

    private BigDecimal price;

    private BigDecimal rating;

    private String distanceLabel;

    private String coverImage;

    private Integer status;
}
