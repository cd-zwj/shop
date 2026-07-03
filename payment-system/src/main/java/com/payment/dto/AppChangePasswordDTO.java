package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户端修改密码数据传输对象，用于在登录态下修改密码。
 */
@Data
public class AppChangePasswordDTO {

    /** 原密码 */
    @NotBlank(message = "原密码不能为空")
    @Size(max = 64, message = "原密码长度不能超过 64 位")
    private String oldPassword;

    /** 新密码，长度 6-64 位 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "新密码长度必须在 6 到 64 位之间")
    private String newPassword;
}
