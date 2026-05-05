package com.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PaymentStatusReasonEnum {
    SALES_ORDER_TIMEOUT_REFUND_REQUIRED(
            "SALES_ORDER_TIMEOUT_REFUND_REQUIRED",
            "Sales order timed out and inventory may have been released; late callback should refund.",
            false,
            PaymentLateCallbackActionEnum.TRIGGER_REFUND
    ),
    SALES_ORDER_CANCELLED_REFUND_REQUIRED(
            "SALES_ORDER_CANCELLED_REFUND_REQUIRED",
            "Sales order was cancelled and inventory may have been released; late callback should refund.",
            false,
            PaymentLateCallbackActionEnum.TRIGGER_REFUND
    ),
    RECHARGE_TIMEOUT_RECOVERABLE(
            "RECHARGE_TIMEOUT_RECOVERABLE",
            "Recharge order timed out locally; late callback can recover business status.",
            true,
            PaymentLateCallbackActionEnum.MARK_SUCCESS
    ),
    MANUAL_REVIEW_REQUIRED(
            "MANUAL_REVIEW_REQUIRED",
            "The payment bill was closed with an unknown reason; late callback requires manual review.",
            false,
            PaymentLateCallbackActionEnum.MANUAL_REVIEW
    );

    private final String code;
    private final String remark;
    private final boolean recoverable;
    private final PaymentLateCallbackActionEnum lateCallbackAction;

    public static PaymentStatusReasonEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
