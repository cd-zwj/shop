package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户端商户搜索结果卡片视图对象，用于展示搜索结果中的商户信息。
 */
@Data
public class AppCatalogSearchTenantVO {

    /** 卡片类型标识，固定为 "tenant" */
    private String type = "tenant";

    /** 搜索结果记录 ID */
    private Long id;

    /** 商户租户 ID */
    private Long tenantId;

    /** 商户标题/展示名 */
    private String title;

    /** 商户名称 */
    private String name;

    /** 商户简介/副标题 */
    private String subtitle;

    /** 商户地址 */
    private String address;

    /** 联系人 */
    private String contact;

    /** 联系电话 */
    private String phone;

    /** 商户分类 */
    private String category;

    /** 商户评分 */
    private BigDecimal rating;

    /** 距离标签（如 1.2km） */
    private String distanceLabel;

    /** 该商户的商品数量 */
    private Long productCount;

    /** 商户状态（0-禁用, 1-正常） */
    private Integer status;
}
