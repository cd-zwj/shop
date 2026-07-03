package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户实体，对应数据库表 sys_user。
 * <p>RBAC 五表权限模型中的用户表，存储平台登录用户的基本信息，
 * 包括账号、密码、联系方式、用户类型及状态等。</p>
 *
 * @deprecated 已迁移至 PlatformUser，本实体仅供历史兼容，新代码请使用 PlatformUser 替代
 */
@Data
@TableName("sys_user")
@Deprecated
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID，用于多租户行级隔离 */
    private Long tenantId;

    /** 登录用户名，全局唯一 */
    private String username;

    /** 登录密码，BCrypt 加密存储，序列化时忽略不返回前端 */
    @JsonIgnore
    private String password;

    /** 用户昵称，用于界面显示 */
    private String nickname;

    /** 手机号码，用于登录验证和消息通知 */
    private String phone;

    /** 电子邮箱，用于找回密码和通知 */
    private String email;

    /** 用户头像URL地址 */
    private String avatar;

    /** 用户类型：1-普通用户，2-管理员 */
    private Integer userType;

    /** 账号状态：0-禁用，1-启用 */
    private Integer status;

    /** 逻辑删除标记：0-未删除，1-已删除 */
    private Integer deleted;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;
}

