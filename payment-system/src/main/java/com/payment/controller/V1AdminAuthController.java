package com.payment.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.payment.common.Result;
import com.payment.dto.V1AdminLoginDTO;
import com.payment.dto.V1AdminSessionVO;
import com.payment.service.AuthCaptchaService;
import com.payment.service.V1AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * v1 管理端认证接口。
 */
@RestController
@RequestMapping("/v1/admin/auth")
@RequiredArgsConstructor
public class V1AdminAuthController {

    private final AuthCaptchaService authCaptchaService;
    private final V1AdminService v1AdminService;

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody V1AdminLoginDTO dto) {
        authCaptchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        return Result.success(v1AdminService.login(dto.getUsername(), dto.getPassword()));
    }

    @GetMapping("/session")
    public Result<V1AdminSessionVO> getSession() {
        return Result.success(v1AdminService.getAdminSession());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }
}
