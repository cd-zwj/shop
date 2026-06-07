package com.payment.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.payment.annotation.RateLimit;
import com.payment.common.Result;
import com.payment.dto.AppUserVO;
import com.payment.dto.PlatformLoginDTO;
import com.payment.dto.PlatformRegisterDTO;
import com.payment.entity.PlatformUser;
import com.payment.service.AuthCaptchaService;
import com.payment.service.LoginSecurityService;
import com.payment.service.PlatformIdentityService;
import com.payment.service.login.PlatformLoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * v1 用户端认证接口。
 */
@RestController
@RequestMapping("/v1/app/auth")
@RequiredArgsConstructor
public class V1AppAuthController {

    private final AuthCaptchaService authCaptchaService;
    private final PlatformIdentityService platformIdentityService;
    private final LoginSecurityService loginSecurityService;

    @RateLimit(prefix = "auth:register", window = 3600, maxRequests = 5, includeIp = true, message = "注册过于频繁，请稍后再试")
    @PostMapping("/register")
    public Result<AppUserVO> register(@Valid @RequestBody PlatformRegisterDTO dto) {
        return Result.success(AppUserVO.toVO(platformIdentityService.register(dto)));
    }

    @RateLimit(prefix = "auth:login", key = "#dto.username", window = 300, maxRequests = 10, includeIp = true, message = "登录尝试过于频繁，请稍后再试")
    @PostMapping("/login/password")
    public Result<String> loginByPassword(@Valid @RequestBody PlatformLoginDTO dto,
                                          HttpServletRequest request) {
        authCaptchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        loginSecurityService.checkNotLocked(dto.getUsername());
        try {
            String token = platformIdentityService.login(
                    PlatformLoginRequest.password(dto.getUsername(), dto.getPassword()));
            loginSecurityService.clearFailures(dto.getUsername());
            return Result.success(token);
        } catch (RuntimeException e) {
            loginSecurityService.recordFailure(dto.getUsername(), request.getRemoteAddr());
            throw e;
        }
    }

    @RateLimit(prefix = "auth:login", key = "#dto.username", window = 300, maxRequests = 10, includeIp = true, message = "登录尝试过于频繁，请稍后再试")
    @PostMapping("/login/sms")
    public Result<String> loginBySms(@Valid @RequestBody PlatformLoginDTO dto,
                                     HttpServletRequest request) {
        authCaptchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        loginSecurityService.checkNotLocked(dto.getUsername());
        try {
            String token = platformIdentityService.login(
                    PlatformLoginRequest.sms(dto.getUsername(), dto.getPassword()));
            loginSecurityService.clearFailures(dto.getUsername());
            return Result.success(token);
        } catch (RuntimeException e) {
            loginSecurityService.recordFailure(dto.getUsername(), request.getRemoteAddr());
            throw e;
        }
    }

    @RateLimit(prefix = "auth:login", key = "#dto.username", window = 300, maxRequests = 10, includeIp = true, message = "登录尝试过于频繁，请稍后再试")
    @PostMapping("/login/third-party")
    public Result<String> loginByThirdParty(@Valid @RequestBody PlatformLoginDTO dto,
                                            HttpServletRequest request) {
        authCaptchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        loginSecurityService.checkNotLocked(dto.getUsername());
        try {
            String token = platformIdentityService.login(
                    PlatformLoginRequest.thirdParty(dto.getUsername(), dto.getPassword()));
            loginSecurityService.clearFailures(dto.getUsername());
            return Result.success(token);
        } catch (RuntimeException e) {
            loginSecurityService.recordFailure(dto.getUsername(), request.getRemoteAddr());
            throw e;
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }
}
