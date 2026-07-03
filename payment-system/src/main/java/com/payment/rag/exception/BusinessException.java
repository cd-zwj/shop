package com.payment.rag.exception;

import lombok.Getter;

/**
 * 业务异常，携带稳定的错误码和用户可看消息。
 * <p>
 * 与 IllegalArgumentException/IllegalStateException 的区别：
 * 此异常的 {@link #getUserMessage()} 安全返回给前端，
 * 而 {@link #getMessage()} 仅用于日志。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final String userMessage;

    public BusinessException(int code, String userMessage) {
        super(userMessage);
        this.code = code;
        this.userMessage = userMessage;
    }

    public BusinessException(int code, String userMessage, String internalMessage) {
        super(internalMessage);
        this.code = code;
        this.userMessage = userMessage;
    }

    public BusinessException(int code, String userMessage, String internalMessage, Throwable cause) {
        super(internalMessage, cause);
        this.code = code;
        this.userMessage = userMessage;
    }
}
