package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderPaymentVO {
    private String orderNo;
    private String orderStatus;
    private String payStatus;
    private BigDecimal totalAmount;
    private BigDecimal unifiedWalletDeductAmount;
    private BigDecimal merchantWalletDeductAmount;
    private BigDecimal externalPayAmount;
    private String paymentBillNo;
    private String externalPayUrl;
}
