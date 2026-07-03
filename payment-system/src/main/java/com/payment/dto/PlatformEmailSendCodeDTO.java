package com.payment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 平台邮箱验证码发送数据传输对象，用于请求向指定邮箱发送验证码。
 */
@Data
public class PlatformEmailSendCodeDTO {

    /** 目标邮箱 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 图形验证码标识（Redis 中的 key） */
    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    /** 用户输入的图形验证码 */
    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}
