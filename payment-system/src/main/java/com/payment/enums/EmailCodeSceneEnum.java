package com.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 邮箱验证码场景枚举。
 *
 * 定义邮箱验证码的使用场景，区分不同业务用途的验证码模板和有效期。
 */
@Getter
@RequiredArgsConstructor
public enum EmailCodeSceneEnum {
    /** 邮箱登录：用于邮箱验证码登录 */
    LOGIN("login", "邮箱登录验证码", "用于邮箱登录"),
    /** 绑定邮箱：用于用户首次绑定或更换邮箱 */
    BIND("bind", "绑定邮箱验证码", "用于绑定邮箱"),
    /** 找回账号：用于找回账号和重置密码 */
    RECOVER("recover", "找回账号验证码", "用于找回账号和重置密码");

    /** 场景编码，用于数据库存储和匹配 */
    private final String code;
    /** 邮件标题 */
    private final String subject;
    /** 操作描述文案，用于邮件正文展示 */
    private final String actionText;
}
