package com.payment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 平台用户密码重置数据传输对象，用于通过邮箱验证码重置密码。
 */
@Data
public class PlatformResetPasswordDTO {

    /** 注册邮箱 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 邮箱验证码 */
    @NotBlank(message = "邮箱验证码不能为空")
    private String emailCode;

    /** 新密码，长度 6-64 位 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在6-64位之间")
    private String newPassword;
}
