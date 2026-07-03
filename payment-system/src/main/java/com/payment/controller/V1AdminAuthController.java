package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.annotation.RateLimit;
import com.payment.common.Result;
import com.payment.config.AuthStpKit;
import com.payment.dto.V1AdminLoginDTO;
import com.payment.dto.V1AdminSessionVO;
import com.payment.service.AuthCaptchaService;
import com.payment.service.V1AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台管理端 - 管理员登录认证控制器。
 * <p>提供管理员登录、会话查询和登出功能，接口路径前缀 /v1/admin/auth。</p>
 * <p>登录接口包含验证码校验和 IP 限流保护，会话和登出接口需管理员已登录。</p>
 */
@RestController
@RequestMapping("/v1/admin/auth")
@RequiredArgsConstructor
public class V1AdminAuthController {

    private final AuthCaptchaService authCaptchaService;
    private final V1AdminService v1AdminService;

    /**
     * 管理员登录。
     * <p>校验验证码后进行用户名密码认证，返回 Sa-Token 令牌。</p>
     * <p>POST /v1/admin/auth/login</p>
     *
     * @param dto 登录请求体，包含用户名、密码和验证码信息
     * @return 登录成功返回 Token 字符串
     */
    @RateLimit(prefix = "admin:auth:login", key = "#dto.username", window = 300, maxRequests = 5, includeIp = true, message = "管理员登录尝试过于频繁，请稍后再试")
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody V1AdminLoginDTO dto) {
        authCaptchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        return Result.success(v1AdminService.login(dto.getUsername(), dto.getPassword()));
    }

    /**
     * 获取当前管理员会话信息。
     * <p>GET /v1/admin/auth/session</p>
     *
     * @return 当前登录管理员的会话详情（V1AdminSessionVO）
     */
    @SaCheckLogin(type = AuthStpKit.ADMIN_TYPE)
    @GetMapping("/session")
    public Result<V1AdminSessionVO> getSession() {
        return Result.success(v1AdminService.getAdminSession());
    }

    /**
     * 管理员登出。
     * <p>注销当前管理员的登录会话，使 Token 失效。</p>
     * <p>POST /v1/admin/auth/logout</p>
     *
     * @return 操作结果
     */
    @SaCheckLogin(type = AuthStpKit.ADMIN_TYPE)
    @PostMapping("/logout")
    public Result<Void> logout() {
        AuthStpKit.ADMIN.logout();
        return Result.success();
    }
}
