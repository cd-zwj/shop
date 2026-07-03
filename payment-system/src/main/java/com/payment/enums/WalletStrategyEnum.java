package com.payment.enums;

/**
 * 钱包支付策略枚举。
 *
 * 定义订单支付时钱包余额的扣款策略，
 * 支持不使用钱包、单一钱包、组合钱包等多种场景。
 */
public enum WalletStrategyEnum {
    /** 不使用钱包：仅通过第三方支付渠道完成支付 */
    NO_WALLET,
    /** 仅使用统一钱包：从全局统一钱包账户扣款 */
    UNIFIED_ONLY,
    /** 仅使用商户钱包：从商户级钱包账户扣款 */
    MERCHANT_ONLY,
    /** 优先商户钱包再统一钱包：先扣商户钱包余额，不足部分从统一钱包扣除 */
    MERCHANT_THEN_UNIFIED,
    /** 优先统一钱包再商户钱包：先扣统一钱包余额，不足部分从商户钱包扣除 */
    UNIFIED_THEN_MERCHANT,
    /** 自定义分摊：按照自定义比例或金额分别从两个钱包扣除 */
    CUSTOM_SPLIT
}
