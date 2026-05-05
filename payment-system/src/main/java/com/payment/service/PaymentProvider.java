package com.payment.service;

import com.payment.dto.ExternalPaymentQueryResult;
import com.payment.dto.PayResponseDTO;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.dto.RefundQueryResultDTO;
import com.payment.dto.RefundRequestDTO;
import com.payment.dto.RefundSubmitResultDTO;
import com.payment.entity.PaymentBill;
import com.payment.entity.RefundRecord;

public interface PaymentProvider {
    String getChannelCode();

    PayResponseDTO createPayment(PaymentBill paymentBill);

    boolean verifyCallback(PaymentCallbackDTO callbackDTO);

    ExternalPaymentQueryResult queryPayment(PaymentBill paymentBill);

    boolean supportsRefund();

    RefundSubmitResultDTO refund(PaymentBill paymentBill, RefundRequestDTO requestDTO);

    RefundQueryResultDTO queryRefund(RefundRecord refundRecord);
}
