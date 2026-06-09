package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.Result;
import com.payment.dto.AppAccountSecurityVO;
import com.payment.dto.AppChangePasswordDTO;
import com.payment.service.AppAccountSecurityService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v1 用户端账号安全接口。
 */
@RestController
@RequestMapping("/v1/app/account-security")
@RequiredArgsConstructor
public class V1AppAccountSecurityController {

    private final AppAccountSecurityService appAccountSecurityService;

    @SaCheckLogin
    @GetMapping("/summary")
    public Result<AppAccountSecurityVO> getSecuritySummary() {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        return Result.success(appAccountSecurityService.getSecuritySummary(platformUserId));
    }

    @SaCheckLogin
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody AppChangePasswordDTO dto) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        appAccountSecurityService.changePassword(platformUserId, dto);
        return Result.success();
    }
}
