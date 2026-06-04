package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商户端短信验证码发送数据对象，用于承载商户端短信验证码发送相关传输数据。
 */
@Data
public class V1MerchantSmsSendCodeDTO {

    @NotBlank(message = "手机号不能为空")
    private String username;

    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}
