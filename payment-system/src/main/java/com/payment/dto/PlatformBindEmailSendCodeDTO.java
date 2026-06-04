package com.payment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 平台Bind邮箱Send验证码数据对象，用于承载平台Bind邮箱Send验证码相关传输数据。
 */
@Data
public class PlatformBindEmailSendCodeDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
