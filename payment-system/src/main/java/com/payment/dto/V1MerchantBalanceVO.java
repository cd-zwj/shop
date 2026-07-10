package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商户端钱包余额视图对象，展示商户级钱包的余额概况。
 */
@Data
public class V1MerchantBalanceVO {

    /** 租户（商户）ID */
    private Long tenantId;

    /** 可用余额 */
    private BigDecimal availableBalance;

    /** 冻结余额（提现审核中的金额） */
    private BigDecimal frozenBalance;

    /** 累计收入总额 */
    private BigDecimal totalIncome;

    /** 累计提现总额 */
    private BigDecimal totalWithdrawal;

    /** 累计平台服务费总额 */
    private BigDecimal totalPlatformFee;
}
