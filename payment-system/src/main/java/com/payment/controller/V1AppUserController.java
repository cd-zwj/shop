package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.Result;
import com.payment.entity.PlatformUser;
import com.payment.service.PlatformIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v1 用户信息接口。
 */
@RestController
@RequestMapping("/v1/app/users")
@RequiredArgsConstructor
public class V1AppUserController {

    private final PlatformIdentityService platformIdentityService;

    @SaCheckLogin
    @GetMapping("/me")
    public Result<PlatformUser> getCurrentUser() {
        return Result.success(platformIdentityService.getCurrentUser());
    }
}
