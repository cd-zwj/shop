package com.payment.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 优惠券适用范围视图对象。
 */
@Data
public class CouponScopeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long couponTemplateId;
    private String scopeType;
    private Long scopeId;
    private String scopeCode;
    private Long tenantId;
}
