package com.payment.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录数据传输对象，用于承载用户名密码登录请求。
 */
@Data
public class LoginDTO {
    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 登录密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}

