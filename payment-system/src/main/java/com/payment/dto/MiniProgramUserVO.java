package com.payment.dto;

import lombok.Data;

/**
 * 微信小程序用户信息视图对象，用于返回小程序登录后的用户信息及 Token。
 */
@Data
public class MiniProgramUserVO {

    /** 用户 ID */
    private Long id;

    /** 用户昵称 */
    private String nickname;

    /** 用户头像 URL */
    private String avatar;

    /** 绑定手机号 */
    private String phone;

    /** JWT 登录令牌 */
    private String token;
}
