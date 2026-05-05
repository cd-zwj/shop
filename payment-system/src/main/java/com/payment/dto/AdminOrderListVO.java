package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminOrderListVO {

    private Long id;
    private String orderNo;
    private Long tenantId;
    private Long platformUserId;
    private String subject;
    private String orderStatus;
    private String payStatus;
    private BigDecimal totalAmount;
    private BigDecimal externalPayAmount;
    private LocalDateTime createTime;
}
