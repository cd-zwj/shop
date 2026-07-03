package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单视图对象，用于返回订单列表的概要信息（V1 App / Merchant 接口）。
 */
@Data
public class SalesOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单 ID */
    private Long id;
    /** 订单编号 */
    private String orderNo;
    /** 商户租户 ID */
    private Long tenantId;
    /** 下单用户 ID */
    private Long platformUserId;
    /** 订单状态（如 PENDING, PAID, COMPLETED, CANCELLED） */
    private String orderStatus;
    /** 支付状态（如 UNPAID, PAID, REFUNDED） */
    private String payStatus;
    /** 订单总金额 */
    private BigDecimal totalAmount;
    /** 优惠券抵扣金额 */
    private BigDecimal discountAmount;
    /** 钱包抵扣总金额 */
    private BigDecimal walletDeductAmount;
    /** 积分抵扣金额 */
    private BigDecimal pointsDeductAmount;
    /** 统一钱包抵扣金额 */
    private BigDecimal unifiedWalletDeductAmount;
    /** 商户钱包抵扣金额 */
    private BigDecimal merchantWalletDeductAmount;
    /** 外部支付金额 */
    private BigDecimal externalPayAmount;
    /** 实际应付金额 */
    private BigDecimal payableAmount;
    /** 订单标题 */
    private String subject;
    /** 订单来源（如 APP, MINI_PROGRAM） */
    private String source;
    /** 使用的钱包支付策略 */
    private String walletStrategy;
    /** 订单过期时间 */
    private LocalDateTime expireTime;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
