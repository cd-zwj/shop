package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class V1MerchantBalanceVO {

    private Long tenantId;

    private BigDecimal availableBalance;

    private BigDecimal frozenBalance;

    private BigDecimal totalIncome;

    private BigDecimal totalWithdrawal;
}
