package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户登录日志。
 */
@Data
@TableName("user_login_log")
public class UserLoginLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long platformUserId;
    /** 登录方式：PASSWORD/SMS/WECHAT/GITHUB/APPLE */
    private String loginType;
    private String loginAccount;
    /** 登录状态：SUCCESS/FAIL */
    private String loginStatus;
    private String failReason;
    private String loginIp;
    private String loginRegion;
    private String deviceId;
    private String userAgent;
    private Integer isUnusual;
    private LocalDateTime createTime;
}
