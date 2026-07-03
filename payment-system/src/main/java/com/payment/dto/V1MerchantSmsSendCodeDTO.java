package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商户端短信验证码发送请求参数，用于承载商户端短信验证码发送相关传输数据。
 */
@Data
public class V1MerchantSmsSendCodeDTO {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    private String username;

    /** 图形验证码标识 */
    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    /** 用户输入的图形验证码 */
    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}
