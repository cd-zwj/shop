package com.payment.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler - 全局异常处理器")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ============================================================
    // 1. handleNotPermissionException
    // ============================================================
    @Nested
    @DisplayName("NotPermissionException 处理")
    class HandleNotPermissionException {

        @Test
        @DisplayName("应返回 403，message 为'无权限访问'，不包含权限码")
        void 应脱敏返回无权限访问() {
            // Arrange
            NotPermissionException ex = new NotPermissionException("order:delete");

            // Act
            Result<?> result = handler.handleNotPermissionException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(403);
            assertThat(result.getMessage()).isEqualTo("无权限访问");
            // 安全断言：返回体中不包含内部权限码
            assertThat(result.getMessage()).doesNotContain("order:delete");
        }

        @Test
        @DisplayName("不同权限码均返回相同脱敏消息")
        void 不同权限码返回相同脱敏消息() {
            // Arrange
            NotPermissionException ex1 = new NotPermissionException("user:export");
            NotPermissionException ex2 = new NotPermissionException("finance:withdraw");

            // Act
            Result<?> result1 = handler.handleNotPermissionException(ex1);
            Result<?> result2 = handler.handleNotPermissionException(ex2);

            // Assert
            assertThat(result1.getMessage()).isEqualTo(result2.getMessage());
        }
    }

    // ============================================================
    // 2. handleNotRoleException
    // ============================================================
    @Nested
    @DisplayName("NotRoleException 处理")
    class HandleNotRoleException {

        @Test
        @DisplayName("应返回 403，message 为'无权限访问'，不包含角色名")
        void 应脱敏返回无权限访问() {
            // Arrange
            NotRoleException ex = new NotRoleException("SUPER_ADMIN");

            // Act
            Result<?> result = handler.handleNotRoleException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(403);
            assertThat(result.getMessage()).isEqualTo("无权限访问");
            // 安全断言：返回体中不包含内部角色名
            assertThat(result.getMessage()).doesNotContain("SUPER_ADMIN");
        }

        @Test
        @DisplayName("不同角色名均返回相同脱敏消息")
        void 不同角色名返回相同脱敏消息() {
            // Arrange
            NotRoleException ex1 = new NotRoleException("ADMIN");
            NotRoleException ex2 = new NotRoleException("MERCHANT");

            // Act
            Result<?> result1 = handler.handleNotRoleException(ex1);
            Result<?> result2 = handler.handleNotRoleException(ex2);

            // Assert
            assertThat(result1.getMessage()).isEqualTo(result2.getMessage());
        }
    }

    // ============================================================
    // 3. handleNotLoginException
    // ============================================================
    @Nested
    @DisplayName("NotLoginException 处理")
    class HandleNotLoginException {

        @Test
        @DisplayName("未提供Token应返回 401")
        void 未提供Token应返回401() {
            // Arrange
            NotLoginException ex = mock(NotLoginException.class);
            when(ex.getType()).thenReturn(NotLoginException.NOT_TOKEN);

            // Act
            Result<?> result = handler.handleNotLoginException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(401);
            assertThat(result.getMessage()).isEqualTo("未提供Token");
        }

        @Test
        @DisplayName("Token无效应返回 401")
        void Token无效应返回401() {
            // Arrange
            NotLoginException ex = mock(NotLoginException.class);
            when(ex.getType()).thenReturn(NotLoginException.INVALID_TOKEN);

            // Act
            Result<?> result = handler.handleNotLoginException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(401);
            assertThat(result.getMessage()).isEqualTo("Token无效");
        }

        @Test
        @DisplayName("Token已过期应返回 401")
        void Token已过期应返回401() {
            // Arrange
            NotLoginException ex = mock(NotLoginException.class);
            when(ex.getType()).thenReturn(NotLoginException.TOKEN_TIMEOUT);

            // Act
            Result<?> result = handler.handleNotLoginException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(401);
            assertThat(result.getMessage()).isEqualTo("Token已过期，请重新登录");
        }

        @Test
        @DisplayName("账号已在其他设备登录应返回 401")
        void 账号已在其他设备登录应返回401() {
            // Arrange
            NotLoginException ex = mock(NotLoginException.class);
            when(ex.getType()).thenReturn(NotLoginException.BE_REPLACED);

            // Act
            Result<?> result = handler.handleNotLoginException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(401);
            assertThat(result.getMessage()).isEqualTo("账号已在其他设备登录");
        }

        @Test
        @DisplayName("账号已被踢下线应返回 401")
        void 账号已被踢下线应返回401() {
            // Arrange
            NotLoginException ex = mock(NotLoginException.class);
            when(ex.getType()).thenReturn(NotLoginException.KICK_OUT);

            // Act
            Result<?> result = handler.handleNotLoginException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(401);
            assertThat(result.getMessage()).isEqualTo("账号已被踢下线");
        }

        @Test
        @DisplayName("未知类型应返回默认消息'请先登录'")
        void 未知类型应返回默认请先登录() {
            // Arrange
            NotLoginException ex = mock(NotLoginException.class);
            when(ex.getType()).thenReturn("UNKNOWN_TYPE");

            // Act
            Result<?> result = handler.handleNotLoginException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(401);
            assertThat(result.getMessage()).isEqualTo("请先登录");
        }
    }

    // ============================================================
    // 4. handleBusinessException
    // ============================================================
    @Nested
    @DisplayName("BusinessException 处理")
    class HandleBusinessException {

        @Test
        @DisplayName("应返回 400 状态码（显式指定）")
        void 显式指定400应返回400() {
            // Arrange
            BusinessException ex = new BusinessException(400, "参数错误");

            // Act
            Result<?> result = handler.handleBusinessException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(400);
            assertThat(result.getMessage()).isEqualTo("参数错误");
        }

        @Test
        @DisplayName("应返回 403 状态码（显式指定）")
        void 显式指定403应返回403() {
            // Arrange
            BusinessException ex = new BusinessException(403, "无权访问该支付单");

            // Act
            Result<?> result = handler.handleBusinessException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(403);
            assertThat(result.getMessage()).isEqualTo("无权访问该支付单");
        }

        @Test
        @DisplayName("仅传 message 时默认 code 为 500（ResultCode.FAIL）")
        void 仅传消息默认code为500() {
            // Arrange
            BusinessException ex = new BusinessException("系统异常");

            // Act
            Result<?> result = handler.handleBusinessException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(500);
            assertThat(result.getMessage()).isEqualTo("系统异常");
        }

        @Test
        @DisplayName("使用 ResultCode 构造时应正确携带 code 和 message")
        void 使用ResultCode构造应正确携带信息() {
            // Arrange
            BusinessException ex = new BusinessException(ResultCode.ORDER_NOT_FOUND);

            // Act
            Result<?> result = handler.handleBusinessException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(ResultCode.ORDER_NOT_FOUND.getCode());
            assertThat(result.getMessage()).isEqualTo(ResultCode.ORDER_NOT_FOUND.getMessage());
        }
    }

    // ============================================================
    // 5. handleException (兜底)
    // ============================================================
    @Nested
    @DisplayName("Exception 兜底处理")
    class HandleException {

        @Test
        @DisplayName("未知异常应返回 500 与通用消息")
        void 未知异常应返回500() {
            // Arrange
            Exception ex = new RuntimeException("unexpected error");

            // Act
            Result<?> result = handler.handleException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(500);
            assertThat(result.getMessage()).isEqualTo("系统异常，请稍后重试");
        }
    }

    // ============================================================
    // 6. handleMethodArgumentNotValidException
    // ============================================================
    @Nested
    @DisplayName("MethodArgumentNotValidException 处理")
    class HandleMethodArgumentNotValidException {

        @Test
        @DisplayName("应返回参数校验失败的字段消息")
        void 应返回字段校验失败消息() {
            // Arrange
            FieldError fieldError = new FieldError("dto", "name", "名称不能为空");
            org.springframework.validation.BindingResult bindingResult =
                    mock(org.springframework.validation.BindingResult.class);
            when(bindingResult.getFieldError()).thenReturn(fieldError);

            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            // Act
            Result<?> result = handler.handleMethodArgumentNotValidException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
            assertThat(result.getMessage()).isEqualTo("名称不能为空");
        }

        @Test
        @DisplayName("无 FieldError 时应返回默认参数校验失败消息")
        void 无FieldError应返回默认消息() {
            // Arrange
            org.springframework.validation.BindingResult bindingResult =
                    mock(org.springframework.validation.BindingResult.class);
            when(bindingResult.getFieldError()).thenReturn(null);

            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            // Act
            Result<?> result = handler.handleMethodArgumentNotValidException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
            assertThat(result.getMessage()).isEqualTo("参数校验失败");
        }
    }

    // ============================================================
    // 7. handleBindException
    // ============================================================
    @Nested
    @DisplayName("BindException 处理")
    class HandleBindException {

        @Test
        @DisplayName("应返回参数校验失败的字段消息")
        void 应返回字段校验失败消息() {
            // Arrange
            FieldError fieldError = new FieldError("dto", "email", "邮箱格式不正确");
            BindException ex = new BindException(new Object(), "dto");
            ex.addError(fieldError);

            // Act
            Result<?> result = handler.handleBindException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
            assertThat(result.getMessage()).isEqualTo("邮箱格式不正确");
        }
    }

    // ============================================================
    // 8. handleConstraintViolationException
    // ============================================================
    @Nested
    @DisplayName("ConstraintViolationException 处理")
    class HandleConstraintViolationException {

        @Test
        @DisplayName("有约束违反时应返回第一条违反消息")
        void 有约束违反应返回首条消息() {
            // Arrange
            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            when(violation.getMessage()).thenReturn("金额必须大于0");

            ConstraintViolationException ex =
                    new ConstraintViolationException(Set.of(violation));

            // Act
            Result<?> result = handler.handleConstraintViolationException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
            assertThat(result.getMessage()).isEqualTo("金额必须大于0");
        }

        @Test
        @DisplayName("无约束违反时应返回默认参数校验失败消息")
        void 无约束违反应返回默认消息() {
            // Arrange
            ConstraintViolationException ex =
                    new ConstraintViolationException(Collections.emptySet());

            // Act
            Result<?> result = handler.handleConstraintViolationException(ex);

            // Assert
            assertThat(result.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
            assertThat(result.getMessage()).isEqualTo("参数校验失败");
        }
    }
}
