package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.payment.common.Result;
import com.payment.dto.LoginDTO;
import com.payment.entity.User;
import com.payment.service.UserService;

import com.payment.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 用户控制器
 */
 
 
@RestController
@RequestMapping("/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDTO dto) {
        String token = userService.login(dto);
        return Result.success(token);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }
    

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody User user) {
        User newUser = userService.register(user);
        return Result.success(newUser);
    }

    @GetMapping("/info")
    @SaCheckLogin  // 需要认证
    public Result<User> getCurrentUser() {
        // 从ThreadLocal中获取用户ID
        Long userId = UserContext.getCurrentUserId();
        User user = userService.getById(userId);
        return Result.success(user);
    }
}

