package com.payment.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 微信小程序登录数据传输对象，用于通过微信授权码登录或注册。
 */
@Data
public class WechatLoginDTO {

    /** 微信授权码（wx.login 获取） */
    @NotBlank(message = "微信code不能为空")
    private String code;

    /** 用户昵称，首次授权时由前端传入 */
    private String nickname;

    /** 用户头像 URL，首次授权时由前端传入 */
    private String avatar;

    /** 手机号，用户授权手机号后由前端传入 */
    private String phone;
}
