package com.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 邮箱验证码SceneEnum枚举，用于定义邮箱验证码SceneEnum相关状态和类型。
 */
@Getter
@RequiredArgsConstructor
public enum EmailCodeSceneEnum {
    /**
     * 处理LOGIN。
     */
    LOGIN("login", "邮箱登录验证码", "用于邮箱登录"),
    BIND("bind", "绑定邮箱验证码", "用于绑定邮箱"),
    RECOVER("recover", "找回账号验证码", "用于找回账号和重置密码");

    private final String code;
    private final String subject;
    private final String actionText;
}
