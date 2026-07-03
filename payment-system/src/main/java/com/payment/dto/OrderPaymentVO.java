package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单支付结果视图对象，用于返回下单后的支付状态和金额明细。
 */
@Data
public class OrderPaymentVO {
    /** 订单编号 */
    private String orderNo;
    /** 订单状态 */
    private String orderStatus;
    /** 支付状态 */
    private String payStatus;
    /** 订单总金额 */
    private BigDecimal totalAmount;
    /** 优惠券抵扣金额 */
    private BigDecimal discountAmount;
    /** 积分抵扣金额 */
    private BigDecimal pointsDeductAmount;
    /** 统一钱包抵扣金额 */
    private BigDecimal unifiedWalletDeductAmount;
    /** 商户钱包抵扣金额 */
    private BigDecimal merchantWalletDeductAmount;
    /** 外部支付（微信/支付宝）金额 */
    private BigDecimal externalPayAmount;
    /** 实际应付金额 */
    private BigDecimal payableAmount;
    /** 支付账单编号 */
    private String paymentBillNo;
    /** 外部支付跳转链接（二维码 URL 或支付链接） */
    private String externalPayUrl;
    /** 是否复用了已有的支付账单 */
    private Boolean reusedPaymentBill;
}
