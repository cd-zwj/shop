package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户登录日志实体，对应数据库表 user_login_log。
 * <p>记录每次用户登录（成功或失败）的详细信息，用于安全审计和异常登录检测。</p>
 */
@Data
@TableName("user_login_log")
public class UserLoginLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的平台用户ID */
    private Long platformUserId;

    /** 登录方式：PASSWORD/SMS/WECHAT/GITHUB/APPLE */
    private String loginType;

    /** 登录账号（手机号、邮箱或第三方账号标识） */
    private String loginAccount;

    /** 登录状态：SUCCESS-成功，FAIL-失败 */
    private String loginStatus;

    /** 登录失败原因描述，登录成功时为空 */
    private String failReason;

    /** 登录时的客户端 IP 地址 */
    private String loginIp;

    /** 根据 IP 解析的登录地区信息 */
    private String loginRegion;

    /** 登录设备唯一标识，用于识别用户常用设备 */
    private String deviceId;

    /** 登录时的浏览器 User-Agent 信息 */
    private String userAgent;

    /** 是否异常登录：0-正常，1-异常（如异地登录、新设备登录等） */
    private Integer isUnusual;

    /** 日志记录创建时间 */
    private LocalDateTime createTime;
}
