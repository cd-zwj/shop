package com.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class V1MerchantCardKeyVO {
    private Long id;
    private Long tenantId;
    private Long productId;
    private String cardCode;
    private String status;
    private String orderNo;
    private Long orderItemId;
    private LocalDateTime usedTime;
    private LocalDateTime returnedTime;
    private String returnReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
