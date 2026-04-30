package com.payment.dto;

import lombok.Data;

@Data
public class PaymentCallbackDTO {
    private String billNo;
    private String callbackRequestId;
    private String thirdPartyBillNo;
    private Boolean success;
    private String rawBody;
}
