package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单主表，对应数据库表 sales_order。
 * <p>
 * 记录 C 端用户在平台上的购买订单，是订单生命周期的核心实体。
 * 订单状态流转：创建 → 支付 → 履约/交付 → 完成/关闭/退款。
 * 支持双钱包、优惠券、积分等多种扣减方式，各扣减金额之和等于应付金额。
 * </p>
 */
@Data
@TableName("sales_order")
public class SalesOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号，全局唯一，业务层面的订单标识 */
    private String orderNo;

    /** 租户 ID，多租户行级隔离字段 */
    private Long tenantId;

    /** 下单用户 ID，关联 platform_user 表 */
    private Long platformUserId;

    /**
     * 订单状态，取值如：PENDING(待支付) / PAID(已支付) / DELIVERING(履约中) /
     * COMPLETED(已完成) / CLOSED(已关闭) / REFUNDING(退款中) / REFUNDED(已退款)
     */
    private String orderStatus;

    /**
     * 支付状态，取值如：UNPAID(未支付) / PAYING(支付中) / PAID(已支付) /
     * PARTIAL_REFUND(部分退款) / FULL_REFUND(全额退款)
     */
    private String payStatus;

    /** 订单总金额（商品单价 x 数量之和），单位：元 */
    private BigDecimal totalAmount;

    /** 优惠减免总金额（含活动折扣 + 优惠券抵扣），单位：元 */
    private BigDecimal discountAmount;

    /** 钱包抵扣总金额（旧字段，保留兼容），单位：元 */
    private BigDecimal walletDeductAmount;

    /** 积分抵扣金额，单位：元 */
    private BigDecimal pointsDeductAmount;

    /** 统一钱包抵扣金额，单位：元 */
    private BigDecimal unifiedWalletDeductAmount;

    /** 商户钱包抵扣金额，单位：元 */
    private BigDecimal merchantWalletDeductAmount;

    /** 外部渠道实际支付金额（微信/支付宝等），单位：元 */
    private BigDecimal externalPayAmount;

    /** 用户实际应付金额 = totalAmount - discountAmount - 各项抵扣，单位：元 */
    private BigDecimal payableAmount;

    /** 订单标题/摘要，用于支付页面展示和第三方支付单描述 */
    private String subject;

    /** 订单来源渠道，如：APP / H5 / MINI_PROGRAM / POS */
    private String source;

    /**
     * 钱包支付策略，取值如：NO_WALLET / UNIFIED_ONLY / MERCHANT_ONLY /
     * MERCHANT_THEN_UNIFIED / UNIFIED_THEN_MERCHANT / CUSTOM_SPLIT
     */
    private String walletStrategy;

    /** 订单过期时间，超时未支付则自动关闭 */
    private LocalDateTime expireTime;

    /**
     * 门店ID
     */
    private Long storeId;

    /** 逻辑删除标记，0-未删除，1-已删除 */
    private Integer deleted;

    /** 订单创建时间 */
    private LocalDateTime createTime;

    /** 订单最后更新时间 */
    private LocalDateTime updateTime;
}
