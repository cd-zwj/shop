package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 第三方认证提供方数据传输对象，用于新增和编辑第三方登录方式（如微信、GitHub）。
 */
@Data
public class PlatformAuthProviderDTO {

    /** 提供方编码（如 WECHAT, GITHUB） */
    @NotBlank(message = "登录方式编码不能为空")
    private String providerCode;

    /** 提供方名称（如 微信登录、GitHub 登录） */
    @NotBlank(message = "登录方式不能为空")
    private String providerName;

    /** 状态（0-禁用, 1-启用） */
    private Integer status;

    /** 排序权重（值越小越靠前） */
    private Integer sortOrder;

    /** 第三方应用 App ID */
    private String appId;

    /** 第三方应用 Client ID */
    private String clientId;

    /** OAuth 回调地址 */
    private String redirectUri;

    /** 扩展配置 JSON（存放额外参数） */
    private String extJson;
}
