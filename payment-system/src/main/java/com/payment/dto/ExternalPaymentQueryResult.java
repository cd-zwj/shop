package com.payment.dto;

import lombok.Data;

@Data
public class ExternalPaymentQueryResult {

    private boolean success;
    private boolean paid;
    private String providerTradeNo;
    private String channelTradeNo;
    private String rawStatus;
    private String message;
    private String buyer;
}
