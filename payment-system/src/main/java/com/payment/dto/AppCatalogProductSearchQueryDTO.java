package com.payment.dto;

import lombok.Data;

/**
 * 用户端商品搜索查询条件数据传输对象，用于公开商品目录的搜索与筛选。
 */
@Data
public class AppCatalogProductSearchQueryDTO {

    /** 当前页码 */
    private Integer current;

    /** 每页条数 */
    private Integer size;

    /** 搜索关键词 */
    private String keyword;

    /** 商品分类筛选 */
    private String category;

    /** 指定商户租户 ID 筛选 */
    private Long tenantId;

    /** 排序方式（如 price_asc, rating_desc） */
    private String sort;
}
