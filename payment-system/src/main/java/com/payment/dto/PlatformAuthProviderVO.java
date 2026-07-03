package com.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 第三方认证提供方视图对象，用于返回第三方登录方式的完整配置信息。
 */
@Data
public class PlatformAuthProviderVO {

    /** 认证提供方 ID */
    private Long id;
    /** 提供方编码（如 WECHAT, GITHUB） */
    private String providerCode;
    /** 提供方名称（如 微信登录、GitHub 登录） */
    private String providerName;
    /** 状态（0-禁用, 1-启用） */
    private Integer status;
    /** 排序权重 */
    private Integer sortOrder;
    /** 第三方应用 App ID */
    private String appId;
    /** 第三方应用 Client ID */
    private String clientId;
    /** OAuth 回调地址 */
    private String redirectUri;
    /** 扩展配置 JSON */
    private String extJson;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
