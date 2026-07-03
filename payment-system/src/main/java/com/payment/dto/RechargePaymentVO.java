package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值支付响应视图对象，包含充值订单信息和第三方支付链接。
 */
@Data
public class RechargePaymentVO {

    /** 充值单号 */
    private String rechargeNo;

    /** 钱包类型（UNIFIED / MERCHANT） */
    private String walletType;

    /** 所属租户 ID */
    private Long tenantId;

    /** 充值金额（元） */
    private BigDecimal rechargeAmount;

    /** 赠送金额（元） */
    private BigDecimal giftAmount;

    /** 赠送积分数 */
    private Integer giftPoints;

    /** 关联的支付账单编号 */
    private String paymentBillNo;

    /** 第三方支付跳转 URL（用于唤起微信/支付宝支付） */
    private String externalPayUrl;
}
