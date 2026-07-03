package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户钱包余额视图对象，用于商户端财务和提现接口。
 */
@Data
public class MerchantBalanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 钱包记录 ID */
    private Long id;

    /** 所属租户（商户）ID */
    private Long tenantId;

    /** 可用余额 */
    private BigDecimal balance;

    /** 冻结余额 */
    private BigDecimal frozenBalance;

    /** 累计收入总额 */
    private BigDecimal totalIncome;

    /** 累计提现总额 */
    private BigDecimal totalWithdrawal;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
