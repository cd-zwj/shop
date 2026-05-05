package com.payment.service;

import com.payment.dto.PayResponseDTO;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.entity.PaymentBill;
import com.payment.enums.PaymentChannelCodeEnum;
import com.payment.enums.PaymentStatusReasonEnum;

import java.math.BigDecimal;

public interface PaymentBillV1Service {
    PaymentBill createBill(String bizType,
                           String bizNo,
                           Long tenantId,
                           Long platformUserId,
                           BigDecimal payAmount,
                           PaymentChannelCodeEnum channelCode);

    PayResponseDTO createExternalPayment(PaymentBill paymentBill);

    void handleCallback(String channelCode, PaymentCallbackDTO callbackDTO);

    void markBizClosed(String bizType, String bizNo, PaymentStatusReasonEnum statusReason);

    PaymentBill getByBillNo(String billNo);

    PaymentBill syncBillStatus(String billNo);
}
