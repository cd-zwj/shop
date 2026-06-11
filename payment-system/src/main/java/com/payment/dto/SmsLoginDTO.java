package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 短信登录请求 DTO。
 */
@Data
public class SmsLoginDTO {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "短信验证码不能为空")
    private String smsCode;

    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}
