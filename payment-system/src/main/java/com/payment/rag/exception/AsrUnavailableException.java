package com.payment.rag.exception;

/**
 * ASR 语音识别服务不可用时抛出。
 * <p>
 * 与"识别结果为空"区分：空结果表示音频中无有效语音，
 * 而此异常表示服务本身不可用（未配置、初始化失败等）。
 */
public class AsrUnavailableException extends RuntimeException {

    public AsrUnavailableException(String message) {
        super(message);
    }

    public AsrUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
