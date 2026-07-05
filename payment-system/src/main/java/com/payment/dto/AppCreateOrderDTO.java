package com.payment.dto;

import com.payment.enums.PaymentChannelCodeEnum;
import com.payment.enums.WalletStrategyEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户端创建订单数据传输对象，用于提交新订单（支持混合支付策略）。
 */
@Data
public class AppCreateOrderDTO {

    /** 商户租户 ID */
    @NotNull(message = "Tenant id is required")
    private Long tenantId;

    /** 订单总金额 */
    @DecimalMin(value = "0.01", message = "Order amount must be greater than 0")
    private BigDecimal totalAmount;

    /** 订单标题/主题 */
    private String subject;

    /** 订单来源（如 APP, MINI_PROGRAM） */
    private String source;

    /** 订单商品项列表 */
    @Valid
    @NotEmpty(message = "Order items are required")
    private List<AppCreateOrderItemDTO> items;

    /** 钱包支付策略（如 UNIFIED_ONLY, MERCHANT_THEN_UNIFIED 等） */
    @NotNull(message = "Wallet strategy is required")
    private WalletStrategyEnum walletStrategy;

    /** 外部支付渠道编码（微信/支付宝等） */
    private PaymentChannelCodeEnum paymentChannelCode;

    /** 统一钱包抵扣金额 */
    private BigDecimal unifiedWalletAmount;
    /** 商户钱包抵扣金额 */
    private BigDecimal merchantWalletAmount;
    /** 外部支付不足时是否允许降级 */
    private Boolean allowExternalPayFallback;
    /** 用户选择的优惠券 ID */
    private Long selectedUserCouponId;
    /** 用户选择的收货地址 ID；实物商品为空时使用默认地址 */
    private Long addressId;
    /** 实际使用积分数 */
    private Integer usedPoints;
    /** 用户期望使用的积分数 */
    private Integer requestedPoints;
}
