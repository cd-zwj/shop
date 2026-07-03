package com.payment.dto;

import lombok.Data;

/**
 * 优惠券适用范围创建数据传输对象，用于为优惠券模板绑定适用的商品或分类范围。
 */
@Data
public class CouponScopeCreateDTO {
    /** 优惠券模板 ID */
    private Long couponTemplateId;
    /** 范围类型（如 PRODUCT-指定商品、CATEGORY-指定分类、TENANT-指定商户） */
    private String scopeType;
    /** 范围对象 ID（商品 ID 或分类 ID） */
    private Long scopeId;
    /** 范围编码（冗余字段，便于查询） */
    private String scopeCode;
    /** 所属商户租户 ID */
    private Long tenantId;
}
