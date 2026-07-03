package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 平台用户实体，对应 platform_user 表。
 * <p>存储 C 端用户的基本注册信息，是平台最核心的用户主表。</p>
 */
@Data
@TableName("platform_user")
public class PlatformUser implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户编号，系统生成的唯一标识，用于对外展示 */
    private String userNo;

    /** 用户名，用户自定义的登录名称 */
    private String username;

    /** 手机号，用于登录和短信验证码 */
    private String phone;

    /** 邮箱地址，可用于登录和找回密码 */
    private String email;

    /** 密码哈希值，BCrypt 等算法加密存储；序列化时忽略，防止泄露 */
    @JsonIgnore
    private String passwordHash;

    /** 邮箱是否已验证：0-未验证，1-已验证 */
    private Integer emailVerified;

    /** 用户状态：0-禁用，1-正常，2-冻结 */
    private Integer status;

    /** 逻辑删除标记：0-未删除，1-已删除 */
    private Integer deleted;

    /** 创建时间（注册时间） */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
