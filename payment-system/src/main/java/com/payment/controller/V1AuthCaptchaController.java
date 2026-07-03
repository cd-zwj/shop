package com.payment.controller;

import com.payment.common.Result;
import com.payment.dto.LoginCaptchaVO;
import com.payment.service.AuthCaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * C端用户验证码控制器。
 * <p>
 * 提供登录图形验证码的生成与获取功能。每次请求会根据客户端 IP 生成
 * 一组新的验证码，用于登录前的人机校验，防止暴力破解攻击。
 * </p>
 * <p>
 * 路径前缀：/v1/auth
 * </p>
 *
 * @author payment-system
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class V1AuthCaptchaController {

    /** 验证码业务服务 */
    private final AuthCaptchaService authCaptchaService;

    /**
     * 获取登录验证码。
     * <p>
     * 生成图形验证码及对应的验证码 ID，返回给前端用于登录时提交校验。
     * 响应头设置为不缓存，确保每次请求都获取全新验证码。
     * </p>
     *
     * @param request HTTP 请求，用于解析客户端真实 IP
     * @return 验证码信息（包含验证码图片 Base64 和验证码 ID），设置 no-store 缓存策略
     */
    @GetMapping("/captcha")
    public ResponseEntity<Result<LoginCaptchaVO>> getCaptcha(HttpServletRequest request) {
        LoginCaptchaVO captcha = authCaptchaService.generateCaptcha(resolveClientIp(request));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate().sMaxAge(0, TimeUnit.SECONDS))
                .body(Result.success(captcha));
    }

    /**
     * 解析客户端真实 IP 地址。
     * <p>
     * 优先从 X-Forwarded-For 头取第一个 IP（经过代理时），其次取 X-Real-IP，
     * 最后回退到 request.getRemoteAddr()。无法识别时返回 "unknown"。
     * </p>
     *
     * @param request HTTP 请求
     * @return 客户端 IP 地址字符串
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr == null || remoteAddr.isBlank()) ? "unknown" : remoteAddr.trim();
    }
}
