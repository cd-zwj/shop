package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家余额视图对象（V1 MerchantFinance / MerchantWithdrawal 接口）
 */
@Data
public class MerchantBalanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private BigDecimal balance;
    private BigDecimal frozenBalance;
    private BigDecimal totalIncome;
    private BigDecimal totalWithdrawal;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
