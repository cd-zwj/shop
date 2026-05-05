package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminPaymentBillVO {

    private Long id;
    private String billNo;
    private String bizType;
    private String bizNo;
    private Long tenantId;
    private Long platformUserId;
    private String channelCode;
    private String payStatus;
    private BigDecimal payAmount;
    private String callbackStatus;
    private String thirdPartyBillNo;
    private LocalDateTime createTime;
}
