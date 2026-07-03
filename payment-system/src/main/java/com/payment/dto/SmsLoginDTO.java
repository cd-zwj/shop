package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 短信验证码登录数据传输对象，用于通过手机号和短信验证码登录。
 */
@Data
public class SmsLoginDTO {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 短信验证码 */
    @NotBlank(message = "短信验证码不能为空")
    private String smsCode;

    /** 图形验证码标识（Redis 中的 key） */
    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    /** 用户输入的图形验证码 */
    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}
