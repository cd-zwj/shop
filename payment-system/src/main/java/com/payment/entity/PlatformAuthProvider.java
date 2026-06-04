package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 第三方登录方式实体类，用于映射并保存第三方登录方式数据。
 */
@Data
@TableName("platform_auth_provider")
public class PlatformAuthProvider implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
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
