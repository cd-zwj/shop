package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RechargePaymentVO {
    private String rechargeNo;
    private String walletType;
    private Long tenantId;
    private BigDecimal rechargeAmount;
    private BigDecimal giftAmount;
    private Integer giftPoints;
    private String paymentBillNo;
    private String externalPayUrl;
}
