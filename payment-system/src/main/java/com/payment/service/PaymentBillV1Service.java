package com.payment.service;

import com.payment.dto.PayResponseDTO;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.entity.PaymentBill;

import java.math.BigDecimal;

public interface PaymentBillV1Service {
    PaymentBill createBill(String bizType, String bizNo, Long tenantId, Long platformUserId, BigDecimal payAmount);

    PayResponseDTO createExternalPayment(PaymentBill paymentBill);

    void handleCallback(String channelCode, PaymentCallbackDTO callbackDTO);

    PaymentBill getByBillNo(String billNo);
}
