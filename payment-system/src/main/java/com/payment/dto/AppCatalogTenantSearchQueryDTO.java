package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户端公开商户搜索查询参数。
 */
@Data
public class AppCatalogTenantSearchQueryDTO {

    private Integer current;

    private Integer size;

    private String keyword;

    private String category;

    private String region;

    private BigDecimal minRating;

    private Integer maxDistanceKm;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String sort;
}
