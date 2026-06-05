package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.payment.annotation.RateLimit;
import com.payment.common.BusinessException;
import com.payment.common.Result;
import com.payment.dto.LoginDTO;
import com.payment.entity.User;
import com.payment.service.UserService;

import com.payment.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 旧版用户控制器 -- 已废弃，请使用 V1 版本接口。
 * @deprecated 使用 {@link V1AppAuthController} 替代
 */
@Slf4j
@RestController
@RequestMapping("/user")
@Deprecated
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * @deprecated 使用 POST /v1/app/auth/login/password
     */
    @Deprecated
    @PostMapping("/login")
    @RateLimit(prefix = "rate:login", key = "#dto.username", window = 300, maxRequests = 10, includeIp = true, message = "登录尝试过于频繁，请5分钟后再试")
    public Result<String> login(@Valid @RequestBody LoginDTO dto) {
        String token = userService.login(dto);
        return Result.success(token);
    }

    @Deprecated
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }

    /**
     * @deprecated 使用 POST /v1/app/auth/register
     */
    @Deprecated
    @PostMapping("/register")
    @RateLimit(prefix = "rate:register", window = 3600, maxRequests = 5, includeIp = true, message = "注册过于频繁，请稍后再试")
    public Result<User> register(@Valid @RequestBody LoginDTO dto) {
        // 旧接口只接受 username + password，不暴露 userType/status 等内部字段
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setNickname(dto.getUsername());
        user.setUserType(1); // 强制普通用户
        user.setStatus(1);
        User newUser = userService.register(user);
        return Result.success(newUser);
    }

    @GetMapping("/info")
    @SaCheckLogin
    public Result<User> getCurrentUser() {
        Long userId = UserContext.getCurrentUserId();
        User user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null); // 不返回密码
        }
        return Result.success(user);
    }
}

