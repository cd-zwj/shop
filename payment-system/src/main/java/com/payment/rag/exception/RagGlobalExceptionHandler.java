package com.payment.rag.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.payment.rag.model.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(basePackages = "com.payment.rag")
public class RagGlobalExceptionHandler {

    // ────── 业务异常（安全返回用户消息） ──────

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Object> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, userMsg={}, internalMsg={}", e.getCode(), e.getUserMessage(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getUserMessage());
    }

    // ────── 参数校验（不泄露内部消息） ──────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Bean Validation 校验失败: {}", message);
        return ApiResponse.validationError(message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数校验失败: {}", e.getMessage(), e);
        // 不透传 getMessage()，避免泄露 SQL 片段、文件路径等内部信息
        return ApiResponse.validationError("请求参数无效，请检查后重试");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ApiResponse<Object> handleIllegalStateException(IllegalStateException e) {
        log.warn("状态异常: {}", e.getMessage(), e);
        return ApiResponse.error(400, "操作状态异常，请稍后重试");
    }

    // ────── 认证/权限 ──────

    @ExceptionHandler(NotLoginException.class)
    public ApiResponse<Object> handleNotLoginException(NotLoginException e) {
        log.warn("用户未登录");
        return ApiResponse.error(401, "未登录或登录已过期");
    }

    @ExceptionHandler(NotPermissionException.class)
    public ApiResponse<Object> handleNotPermissionException(NotPermissionException e) {
        log.warn("用户权限不足，缺少权限: {}", e.getPermission());
        return ApiResponse.error(403, "无权访问该接口");
    }

    // ────── 服务不可用 ──────

    @ExceptionHandler(AsrUnavailableException.class)
    public ApiResponse<Object> handleAsrUnavailableException(AsrUnavailableException e) {
        log.warn("ASR 服务不可用: {}", e.getMessage());
        return ApiResponse.error(503, "语音识别服务暂不可用，请稍后重试");
    }

    @ExceptionHandler(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class)
    public ApiResponse<Object> handleCallNotPermittedException(io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
        log.warn("熔断器已打开，拒绝调用: {}", e.getMessage());
        return ApiResponse.error(503, "服务暂时不可用，请稍后重试");
    }

    // ────── 文件上传 ──────

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ApiResponse<Object> handleMaxSizeException(org.springframework.web.multipart.MaxUploadSizeExceededException e) {
        log.warn("文件上传大小超出限制");
        return ApiResponse.error(413, "文件大小超过限制，请上传更小的文件");
    }

    // ────── 兜底 ──────

    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleException(Exception e) {
        log.error("系统未处理的异常: ", e);
        return ApiResponse.serverError("服务器内部错误，请稍后重试");
    }
}
