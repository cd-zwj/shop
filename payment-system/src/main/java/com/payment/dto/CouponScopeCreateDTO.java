package com.payment.dto;

import lombok.Data;

/**
 * 优惠券适用范围创建参数。
 */
@Data
public class CouponScopeCreateDTO {
    private Long couponTemplateId;
    private String scopeType;
    private Long scopeId;
    private String scopeCode;
    private Long tenantId;
}
