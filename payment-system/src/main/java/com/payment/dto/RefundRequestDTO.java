package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequestDTO {
    private String refundNo;
    private BigDecimal refundAmount;
    private String refundReason;
}
