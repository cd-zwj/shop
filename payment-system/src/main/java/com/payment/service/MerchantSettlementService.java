package com.payment.service;

import java.math.BigDecimal;

/**
 * 商户订单结算服务。
 * <p>
 * 负责订单支付成功后的商户入账，并按租户配置的平台服务费率抽成后入账净额。
 * 同时写入商户钱包流水，并在流水与商户余额中记录平台服务费。
 * 抽成比例存储在 tenant_config 中，key 为 PLATFORM_FEE_RATE，值是 0-100 的百分比数字（如 "3" 表示 3%）。
 */
public interface MerchantSettlementService {

    /**
     * 结算单笔订单的商户入账金额。
     * <p>
     * 读取租户配置的 PLATFORM_FEE_RATE（缺省为 0），按 {@code settlementAmount * rate / 100} 计算服务费，
     * 将净额（{@code settlementAmount - fee}）记入商户可用余额，
     * 并写入带平台服务费金额的订单收款流水。
     *
     * @param tenantId        商户租户 ID
     * @param settlementAmount 订单应付金额扣除商户钱包抵扣后的结算基数，必须 > 0
     * @param orderNo         关联订单号，用于流水追踪
     * @return 实际入账净额（结算金额 - 服务费）
     */
    BigDecimal settleOrder(Long tenantId, BigDecimal settlementAmount, String orderNo);

    /**
     * 读取租户当前生效的平台服务费率（0-100 的百分比数字，默认 0）。
     *
     * @param tenantId 租户 ID
     * @return 抽成百分比，例如 3 表示 3%
     */
    BigDecimal getFeeRatePercent(Long tenantId);
}
