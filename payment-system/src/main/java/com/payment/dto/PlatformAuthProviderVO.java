package com.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 第三方登录方式视图对象，用于返回第三方登录方式展示数据。
 */
@Data
public class PlatformAuthProviderVO {

    private Long id;
    private String providerCode;
    private String providerName;
    private Integer status;
    private Integer sortOrder;
    private String appId;
    private String clientId;
    private String redirectUri;
    private String extJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
