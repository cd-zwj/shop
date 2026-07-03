package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台管理端数据看板总览视图对象，展示全平台核心运营指标汇总。
 */
@Data
public class AdminDashboardOverviewVO {

    /** 平台注册用户总数 */
    private Long totalPlatformUsers;

    /** 商户（租户）总数 */
    private Long totalMerchants;

    /** 当前活跃商户数（状态为启用的商户） */
    private Long activeMerchants;

    /** 订单总数 */
    private Long totalOrders;

    /** 已支付订单数 */
    private Long paidOrders;

    /** 订单总金额 */
    private BigDecimal totalOrderAmount;

    /** 支付账单总数 */
    private Long totalPaymentBills;

    /** 支付总金额 */
    private BigDecimal totalPaymentAmount;

    /** 充值订单总数 */
    private Long totalRechargeOrders;

    /** 充值总金额 */
    private BigDecimal totalRechargeAmount;

    /** 待处理提现申请数 */
    private Long pendingWithdrawals;
}
