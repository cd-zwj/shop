package com.payment.common;

/**
 * 统一结果码枚举
 * <p>
 * 定义系统中所有业务场景的状态码及对应的提示信息，
 * 用于 {@link Result} 和 {@link BusinessException} 中标识具体业务结果。
 * </p>
 *
 * <p>错误码分段规则：</p>
 * <ul>
 *   <li>200-499：HTTP 标准状态码</li>
 *   <li>1001-1999：支付相关错误码</li>
 *   <li>2001-2999：用户相关错误码</li>
 * </ul>
 *
 * @author payment-system
 */
public enum ResultCode {
    /** 操作成功 */
    SUCCESS(200, "操作成功"),
    /** 操作失败 */
    FAIL(500, "操作失败"),
    /** 未授权 */
    UNAUTHORIZED(401, "未授权"),
    /** 禁止访问 */
    FORBIDDEN(403, "禁止访问"),
    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),
    /** 参数错误 */
    PARAM_ERROR(400, "参数错误"),
    /** 支付失败 */
    PAYMENT_ERROR(1001, "支付失败"),
    /** 支付通知处理失败 */
    PAYMENT_NOTIFY_ERROR(1002, "支付通知处理失败"),
    /** 订单不存在 */
    ORDER_NOT_FOUND(1003, "订单不存在"),
    /** 订单状态错误 */
    ORDER_STATUS_ERROR(1004, "订单状态错误"),
    /** 用户不存在 */
    USER_NOT_FOUND(2001, "用户不存在"),
    /** 用户已禁用 */
    USER_DISABLED(2002, "用户已禁用"),
    /** 余额不足 */
    BALANCE_INSUFFICIENT(2003, "余额不足"),
    /** 用户注册信息冲突 */
    USER_ALREADY_EXISTS(2004, "用户已存在");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 提示信息
     */
    private final String message;

    /**
     * 构造结果码枚举
     *
     * @param code    错误码
     * @param message 提示信息
     */
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取提示信息
     *
     * @return 提示信息
     */
    public String getMessage() {
        return message;
    }
}
