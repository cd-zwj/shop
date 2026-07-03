package com.payment.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;

/**
 * 全局异常处理器
 * <p>
 * 统一拦截并处理各类异常，将异常转换为标准 {@link Result} 响应格式返回给前端。
 * 覆盖 Sa-Token 认证异常、业务异常、参数校验异常及系统未知异常。
 * </p>
 *
 * @author payment-system
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 Sa-Token 未登录异常
     * <p>
     * 根据异常类型细分提示信息：未提供 Token、Token 无效、Token 过期、
     * 账号被替换、账号被踢下线等。
     * </p>
     *
     * @param e Sa-Token 未登录异常
     * @return 包含 401 状态码的统一响应
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<?> handleNotLoginException(NotLoginException e) {
        log.warn("未登录访问: {}", e.getMessage());
        String message;
        switch (e.getType()) {
            case NotLoginException.NOT_TOKEN:
                message = "未提供Token";
                break;
            case NotLoginException.INVALID_TOKEN:
                message = "Token无效";
                break;
            case NotLoginException.TOKEN_TIMEOUT:
                message = "Token已过期，请重新登录";
                break;
            case NotLoginException.BE_REPLACED:
                message = "账号已在其他设备登录";
                break;
            case NotLoginException.KICK_OUT:
                message = "账号已被踢下线";
                break;
            default:
                message = "请先登录";
        }
        return Result.error(401, message);
    }

    /**
     * 处理 Sa-Token 无权限异常（不暴露内部权限码）
     *
     * @param e Sa-Token 权限不足异常
     * @return 包含 403 状态码的统一响应
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<?> handleNotPermissionException(NotPermissionException e) {
        log.warn("权限不足，权限码: {}", e.getPermission());
        return Result.error(403, "无权限访问");
    }

    /**
     * 处理 Sa-Token 无角色异常（不暴露内部角色名）
     *
     * @param e Sa-Token 角色不足异常
     * @return 包含 403 状态码的统一响应
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<?> handleNotRoleException(NotRoleException e) {
        log.warn("角色权限不足，角色: {}", e.getRole());
        return Result.error(403, "无权限访问");
    }

    /**
     * 处理未知系统异常（兜底）
     *
     * @param e 系统异常
     * @return 包含 500 状态码的统一响应
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统异常，请稍后重试");
    }

    /**
     * 处理业务异常
     *
     * @param e 业务异常
     * @return 包含对应业务错误码的统一响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理 @RequestBody 参数校验异常
     *
     * @param e 方法参数校验异常
     * @return 包含 400 状态码及首个字段错误信息的统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return Result.error(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理表单绑定异常
     *
     * @param e 绑定异常
     * @return 包含 400 状态码及首个字段错误信息的统一响应
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return Result.error(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理 JSR 303/380 约束违反异常
     *
     * @param e 约束违反异常
     * @return 包含 400 状态码及首个违反信息的统一响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = violations.isEmpty() ? "参数校验失败" : violations.iterator().next().getMessage();
        return Result.error(ResultCode.PARAM_ERROR.getCode(), message);
    }
}
