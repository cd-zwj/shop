package com.payment.service;

import com.payment.dto.PaymentCallbackDTO;
import com.payment.enums.PaymentCallbackFailureReasonEnum;

/** 持久化被拒绝的支付回调安全审计。 */
public interface PaymentCallbackAuditService {

    void recordRejected(String channelCode,
                        PaymentCallbackDTO callbackDTO,
                        PaymentCallbackFailureReasonEnum failureReason);
}
