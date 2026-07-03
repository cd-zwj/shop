package com.payment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 平台用户绑定邮箱发送验证码数据传输对象，用于请求向待绑定邮箱发送验证码。
 */
@Data
public class PlatformBindEmailSendCodeDTO {

    /** 待绑定的邮箱 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
