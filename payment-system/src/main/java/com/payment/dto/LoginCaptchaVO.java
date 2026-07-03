package com.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录验证码视图对象，用于返回图形验证码的标识和图片数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginCaptchaVO {

    /** 验证码标识（与 Redis 中存储的验证码关联） */
    private String captchaKey;

    /** 验证码图片（Base64 编码） */
    private String captchaImage;
}
