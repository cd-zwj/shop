package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送短信验证码数据传输对象，用于请求向指定手机号发送短信验证码。
 */
@Data
public class SmsSendCodeDTO {

    /** 目标手机号 */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 图形验证码标识（Redis 中的 key） */
    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    /** 用户输入的图形验证码 */
    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}
