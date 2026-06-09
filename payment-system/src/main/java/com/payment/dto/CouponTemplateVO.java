package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板视图对象（Admin 和 Merchant 共用）。
 */
@Data
public class CouponTemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String templateNo;
    private Long tenantId;
    private String ownerType;
    private String name;
    private String couponType;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal maxDiscountAmount;
    private Integer totalStock;
    private Integer receivedCount;
    private Integer usedQuantity;
    private Integer perUserLimit;
    private LocalDateTime receiveStartTime;
    private LocalDateTime receiveEndTime;
    private Integer validDaysAfterReceive;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    private Integer minMemberLevel;
    private String excludeMemberTagIds;
    private String stackStrategy;
    private String description;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
