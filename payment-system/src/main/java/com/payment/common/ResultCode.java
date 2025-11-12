package com.payment.common;

public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    PARAM_ERROR(400, "参数错误"),
    PAYMENT_ERROR(1001, "支付失败"),
    PAYMENT_NOTIFY_ERROR(1002, "支付通知处理失败"),
    ORDER_NOT_FOUND(1003, "订单不存在"),
    ORDER_STATUS_ERROR(1004, "订单状态错误"),
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_DISABLED(2002, "用户已禁用"),
    BALANCE_INSUFFICIENT(2003, "余额不足");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

