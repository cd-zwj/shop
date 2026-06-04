package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 第三方登录方式数据对象，用于承载第三方登录方式相关传输数据。
 */
@Data
public class PlatformAuthProviderDTO {

    @NotBlank(message = "登录方式编码不能为空")
    private String providerCode;

    @NotBlank(message = "登录方式名称不能为空")
    private String providerName;

    private Integer status;

    private Integer sortOrder;

    private String appId;

    private String clientId;

    private String redirectUri;

    private String extJson;
}
