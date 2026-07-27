package com.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 支付回调拒绝原因，仅使用固定枚举，避免把异常详情写入审计库。 */
@Getter
@RequiredArgsConstructor
public enum PaymentCallbackFailureReasonEnum {
    SIGNATURE_INVALID(false),
    SIGNATURE_VERIFICATION_ERROR(false),
    SIGNED_PAYLOAD_MISMATCH(true),
    SIGNED_BILL_NOT_FOUND(true),
    IDEMPOTENCY_CONFLICT(true),
    PAYLOAD_INVALID(false);

    private final boolean signatureVerified;
}
