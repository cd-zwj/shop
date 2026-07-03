package com.payment.common;

import lombok.Getter;

/**
 * 业务异常类
 * <p>
 * 用于封装业务逻辑中可预见的异常情况，
 * 包含错误码和错误信息，由 {@link GlobalExceptionHandler} 统一捕获并返回。
 * </p>
 *
 * @author payment-system
 */
@Getter
public class BusinessException extends RuntimeException {
    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 构造业务异常（使用默认错误码 {@link ResultCode#FAIL}）
     *
     * @param message 错误信息
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.FAIL.getCode();
    }

    /**
     * 构造业务异常（自定义错误码）
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造业务异常（使用预定义结果码枚举）
     *
     * @param resultCode 结果码枚举
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }
}
