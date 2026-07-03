package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 钱包账户视图对象，用于返回用户钱包的余额概览信息。
 */
@Data
public class WalletAccountVO {
    /** 钱包类型（如 UNIFIED-统一钱包、MERCHANT-商户钱包） */
    private String walletType;
    /** 所属商户租户 ID（统一钱包为 null） */
    private Long tenantId;
    /** 可用余额 */
    private BigDecimal availableAmount;
    /** 冻结金额 */
    private BigDecimal frozenAmount;
    /** 累计充值金额 */
    private BigDecimal totalRecharge;
    /** 累计消费金额 */
    private BigDecimal totalConsume;
}
