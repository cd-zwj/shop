package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台管理端交易总览视图对象，展示订单、支付账单、充值三大维度的统计数据。
 */
@Data
public class AdminTradeOverviewVO {

    /** 订单总数 */
    private Long totalOrders;

    /** 已支付订单数 */
    private Long paidOrders;

    /** 待支付订单数 */
    private Long pendingOrders;

    /** 订单总金额 */
    private BigDecimal totalOrderAmount;

    /** 外部支付总金额（第三方渠道实际支付） */
    private BigDecimal totalExternalPayAmount;

    /** 支付账单总数 */
    private Long totalPaymentBills;

    /** 已支付账单数 */
    private Long paidPaymentBills;

    /** 支付账单总金额 */
    private BigDecimal totalPaymentAmount;

    /** 充值订单总数 */
    private Long totalRechargeOrders;

    /** 成功充值订单数 */
    private Long successRechargeOrders;

    /** 充值总金额 */
    private BigDecimal totalRechargeAmount;
}
