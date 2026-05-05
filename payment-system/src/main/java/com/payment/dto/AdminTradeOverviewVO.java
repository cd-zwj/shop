package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminTradeOverviewVO {

    private Long totalOrders;
    private Long paidOrders;
    private Long pendingOrders;
    private BigDecimal totalOrderAmount;
    private BigDecimal totalExternalPayAmount;

    private Long totalPaymentBills;
    private Long paidPaymentBills;
    private BigDecimal totalPaymentAmount;

    private Long totalRechargeOrders;
    private Long successRechargeOrders;
    private BigDecimal totalRechargeAmount;
}
