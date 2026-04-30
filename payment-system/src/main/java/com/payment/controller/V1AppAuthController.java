package com.payment.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.payment.common.Result;
import com.payment.dto.PlatformLoginDTO;
import com.payment.dto.PlatformRegisterDTO;
import com.payment.entity.PlatformUser;
import com.payment.service.PlatformIdentityService;
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

    private final PlatformIdentityService platformIdentityService;

    @PostMapping("/register")
    public Result<PlatformUser> register(@Valid @RequestBody PlatformRegisterDTO dto) {
        return Result.success(platformIdentityService.register(dto));
    }

    @PostMapping("/login/password")
    public Result<String> loginByPassword(@Valid @RequestBody PlatformLoginDTO dto) {
        return Result.success(platformIdentityService.login(dto));
    }

    @PostMapping("/login/sms")
    public Result<String> loginBySms(@RequestBody PlatformLoginDTO dto) {
        return Result.success(platformIdentityService.login(dto));
    }

    @PostMapping("/login/third-party")
    public Result<String> loginByThirdParty(@RequestBody PlatformLoginDTO dto) {
        return Result.success(platformIdentityService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }
}
