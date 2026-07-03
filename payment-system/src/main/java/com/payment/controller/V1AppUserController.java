package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.Result;
import com.payment.dto.AppUserVO;
import com.payment.service.PlatformIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C端用户信息控制器。
 * <p>
 * 提供当前登录用户基本信息查询接口，需要platform端登录态。
 * <p>
 * 路径前缀：/v1/app/users，需要platform用户登录。
 *
 * @author payment-system
 */
@RestController
@RequestMapping("/v1/app/users")
@RequiredArgsConstructor
public class V1AppUserController {

    private final PlatformIdentityService platformIdentityService;

    /**
     * 获取当前登录用户信息。
     * <p>
     * 返回当前登录用户的个人信息，包括用户ID、昵称、头像、邮箱、手机号等。
     *
     * @return 当前用户信息VO
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/me")
    public Result<AppUserVO> getCurrentUser() {
        return Result.success(AppUserVO.toVO(platformIdentityService.getCurrentUser()));
    }
}
