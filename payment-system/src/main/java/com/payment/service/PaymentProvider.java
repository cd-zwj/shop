package com.payment.service;

import com.payment.dto.PayResponseDTO;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.entity.PaymentBill;

public interface PaymentProvider {
    String getChannelCode();

    PayResponseDTO createPayment(PaymentBill paymentBill);

    boolean verifyCallback(PaymentCallbackDTO callbackDTO);
}
