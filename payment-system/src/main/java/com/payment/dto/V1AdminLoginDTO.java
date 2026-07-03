package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 平台管理端登录请求参数。
 */
@Data
public class V1AdminLoginDTO {

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码标识（用于从 Redis 中匹配验证码） */
    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    /** 用户输入的图形验证码 */
    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}
