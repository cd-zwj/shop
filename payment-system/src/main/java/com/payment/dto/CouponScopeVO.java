package com.payment.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 优惠券适用范围视图对象，用于返回优惠券模板关联的适用范围信息。
 */
@Data
public class CouponScopeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 范围记录 ID */
    private Long id;
    /** 优惠券模板 ID */
    private Long couponTemplateId;
    /** 范围类型（如 PRODUCT、CATEGORY、TENANT） */
    private String scopeType;
    /** 范围对象 ID */
    private Long scopeId;
    /** 范围编码 */
    private String scopeCode;
    /** 所属商户租户 ID */
    private Long tenantId;
}
