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

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class V1AuthCaptchaController {

    private final AuthCaptchaService authCaptchaService;

    @GetMapping("/captcha")
    public ResponseEntity<Result<LoginCaptchaVO>> getCaptcha(HttpServletRequest request) {
        LoginCaptchaVO captcha = authCaptchaService.generateCaptcha(resolveClientIp(request));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate().sMaxAge(0, TimeUnit.SECONDS))
                .body(Result.success(captcha));
    }

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
