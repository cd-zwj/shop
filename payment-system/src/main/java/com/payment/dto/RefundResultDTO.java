package com.payment.dto;

import lombok.Data;

@Data
public class RefundResultDTO {
    private boolean success;
    private String channelStatus;
    private String providerRefundNo;
    private String rawStatus;
    private String message;
}
