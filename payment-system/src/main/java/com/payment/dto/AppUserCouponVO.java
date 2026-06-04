package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户端券包视图对象。
 */
@Data
public class AppUserCouponVO {
    private Long id;
    private String couponNo;
    private Long couponTemplateId;
    private Long tenantId;
    private String status;
    private String name;
    private String couponType;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime receiveTime;
    private LocalDateTime expireTime;
    private LocalDateTime usedTime;
}
