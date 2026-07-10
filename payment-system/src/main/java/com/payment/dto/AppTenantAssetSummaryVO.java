package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户端按商户聚合的资产概览。
 */
@Data
public class AppTenantAssetSummaryVO {

    private Long tenantId;
    private String tenantName;
    private Integer memberStatus;
    private BigDecimal walletAvailableAmount = BigDecimal.ZERO;
    private BigDecimal walletFrozenAmount = BigDecimal.ZERO;
    private Integer points = 0;
    private Integer expiringSoonPoints = 0;
    private Integer usableCouponCount = 0;
    private Integer lockedCouponCount = 0;
    private Integer usedCouponCount = 0;
    private Integer expiredCouponCount = 0;
    private Integer totalGrowth = 0;
}
