package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户端可领取优惠券视图对象。
 */
@Data
public class AppCouponTemplateVO {
    private Long id;
    private Long tenantId;
    private String ownerType;
    private String name;
    private String couponType;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal maxDiscountAmount;
    private Integer perUserLimit;
    private Integer remainingStock;
    private Integer receivedByCurrentUser;
    private Boolean receivable;
    private LocalDateTime receiveStartTime;
    private LocalDateTime receiveEndTime;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    private Integer validDaysAfterReceive;
    private String description;
}
