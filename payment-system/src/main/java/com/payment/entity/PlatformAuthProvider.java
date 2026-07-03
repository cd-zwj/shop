package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 平台认证提供方实体，对应 platform_auth_provider 表。
 * <p>管理第三方登录方式的配置信息，如微信、支付宝、GitHub 等 OAuth 服务。
 * 通过此表可动态配置和开关第三方登录渠道，无需修改代码。</p>
 */
@Data
@TableName("platform_auth_provider")
public class PlatformAuthProvider implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提供方编码，唯一标识，如 WECHAT、ALIPAY、GITHUB 等 */
    private String providerCode;

    /** 提供方名称，用于前端展示，如"微信登录"、"支付宝登录" */
    private String providerName;

    /** 状态：0-禁用，1-启用；禁用后该第三方登录渠道不可用 */
    private Integer status;

    /** 排序权重，数值越小越靠前，用于控制前端登录按钮的展示顺序 */
    private Integer sortOrder;

    /** 应用ID（appId），第三方 OAuth 应用的唯一标识 */
    private String appId;

    /** 客户端ID（clientId），OAuth 2.0 协议中的客户端标识 */
    private String clientId;

    /** 重定向URI（redirectUri），OAuth 授权回调地址 */
    private String redirectUri;

    /** 扩展配置（JSON 格式），存储 appSecret、scope、自定义参数等敏感或附加配置 */
    private String extJson;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
