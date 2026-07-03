package com.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 支付状态原因枚举。
 *
 * 描述支付账单状态异常（如超时、取消）的具体原因，
 * 并定义每种原因对应的延迟回调处理动作。
 * 用于支付延迟回调（Late Callback）的决策路由。
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatusReasonEnum {
    /** 销售订单超时需退款：订单已超时且库存可能已释放，延迟回调应触发退款 */
    SALES_ORDER_TIMEOUT_REFUND_REQUIRED(
            "SALES_ORDER_TIMEOUT_REFUND_REQUIRED",
            "Sales order timed out and inventory may have been released; late callback should refund.",
            false,
            PaymentLateCallbackActionEnum.TRIGGER_REFUND
    ),
    /** 销售订单取消需退款：订单已被取消且库存可能已释放，延迟回调应触发退款 */
    SALES_ORDER_CANCELLED_REFUND_REQUIRED(
            "SALES_ORDER_CANCELLED_REFUND_REQUIRED",
            "Sales order was cancelled and inventory may have been released; late callback should refund.",
            false,
            PaymentLateCallbackActionEnum.TRIGGER_REFUND
    ),
    /** 充值订单超时可恢复：充值订单本地超时，延迟回调可恢复业务状态 */
    RECHARGE_TIMEOUT_RECOVERABLE(
            "RECHARGE_TIMEOUT_RECOVERABLE",
            "Recharge order timed out locally; late callback can recover business status.",
            true,
            PaymentLateCallbackActionEnum.MARK_SUCCESS
    ),
    /** 需人工审核：支付账单因未知原因关闭，延迟回调需人工介入审核 */
    MANUAL_REVIEW_REQUIRED(
            "MANUAL_REVIEW_REQUIRED",
            "The payment bill was closed with an unknown reason; late callback requires manual review.",
            false,
            PaymentLateCallbackActionEnum.MANUAL_REVIEW
    );

    /** 原因编码，用于数据库存储和匹配 */
    private final String code;
    /** 备注说明，描述该原因的具体含义 */
    private final String remark;
    /** 是否可恢复：true 表示业务状态可自动恢复，false 表示需要退款或人工介入 */
    private final boolean recoverable;
    /** 延迟回调时应执行的动作 */
    private final PaymentLateCallbackActionEnum lateCallbackAction;

    /**
     * 根据编码查找对应的枚举值。
     *
     * @param code 原因编码
     * @return 对应的枚举值，未找到时返回 null
     */
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
