package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户端统一商户搜索卡片数据。
 */
@Data
public class AppCatalogSearchTenantVO {

    private String type = "tenant";

    private Long id;

    private Long tenantId;

    private String title;

    private String name;

    private String subtitle;

    private String address;

    private String contact;

    private String phone;

    private String category;

    private BigDecimal rating;

    private String distanceLabel;

    private Long productCount;

    private Integer status;
}
