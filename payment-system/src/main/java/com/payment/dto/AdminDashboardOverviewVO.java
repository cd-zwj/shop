package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminDashboardOverviewVO {

    private Long totalPlatformUsers;
    private Long totalMerchants;
    private Long activeMerchants;
    private Long totalOrders;
    private Long paidOrders;
    private BigDecimal totalOrderAmount;
    private Long totalPaymentBills;
    private BigDecimal totalPaymentAmount;
    private Long totalRechargeOrders;
    private BigDecimal totalRechargeAmount;
    private Long pendingWithdrawals;
}
