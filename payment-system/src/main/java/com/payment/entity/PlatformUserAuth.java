package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户认证信息实体，对应 platform_user_auth 表。
 * <p>存储用户的各种登录认证方式（手机号、邮箱、微信、支付宝等第三方 OAuth），
 * 一个用户可绑定多种认证方式，实现多渠道登录。</p>
 */
@Data
@TableName("platform_user_auth")
public class PlatformUserAuth implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的平台用户ID，对应 platform_user.id */
    private Long platformUserId;

    /** 认证类型：PASSWORD-密码登录，PHONE-手机验证码，WECHAT-微信登录，ALIPAY-支付宝登录 等 */
    private String authType;

    /** 认证凭证标识：密码登录时为手机号/邮箱，第三方登录时为 openId/unionId */
    private String authKey;

    /** 关联的认证提供方ID，对应 platform_auth_provider.id；非第三方登录时为空 */
    private Long providerId;

    /** 联合唯一键，用于在第三方场景下标识唯一用户（如微信 unionId），避免重复绑定 */
    private String authUnionKey;

    /** 扩展信息（JSON 格式），存储第三方返回的额外数据，如头像、昵称、access_token 等 */
    private String extraJson;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
