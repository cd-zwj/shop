package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户端商户搜索查询条件数据传输对象，用于公开商户目录的搜索与筛选（支持地理位置）。
 */
@Data
public class AppCatalogTenantSearchQueryDTO {

    /** 当前页码 */
    private Integer current;

    /** 每页条数 */
    private Integer size;

    /** 搜索关键词 */
    private String keyword;

    /** 商户分类筛选 */
    private String category;

    /** 区域筛选（如城市名） */
    private String region;

    /** 最低评分筛选 */
    private BigDecimal minRating;

    /** 最大距离（公里） */
    private Integer maxDistanceKm;

    /** 用户经度（用于距离排序） */
    private BigDecimal longitude;

    /** 用户纬度（用于距离排序） */
    private BigDecimal latitude;

    /** 排序方式（如 distance_asc, rating_desc） */
    private String sort;
}
