package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户端修改密码数据对象，用于承载登录态内密码修改请求。
 */
@Data
public class AppChangePasswordDTO {

    @NotBlank(message = "原密码不能为空")
    @Size(max = 64, message = "原密码长度不能超过 64 位")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "新密码长度必须在 6 到 64 位之间")
    private String newPassword;
}
