package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletAccountVO {
    private String walletType;
    private Long tenantId;
    private BigDecimal availableAmount;
    private BigDecimal frozenAmount;
    private BigDecimal totalRecharge;
    private BigDecimal totalConsume;
}
