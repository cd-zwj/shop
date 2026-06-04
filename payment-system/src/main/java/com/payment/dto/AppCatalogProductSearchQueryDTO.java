package com.payment.dto;

import lombok.Data;

/**
 * 用户端公开商品搜索查询参数。
 */
@Data
public class AppCatalogProductSearchQueryDTO {

    private Integer current;

    private Integer size;

    private String keyword;

    private String category;

    private Long tenantId;

    private String sort;
}
