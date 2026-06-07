package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录失败记录实体
 */
@Data
@TableName("login_fail_record")
public class LoginFailRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 登录账号
     */
    private String account;

    /**
     * 登录IP
     */
    private String ip;

    /**
     * 连续失败次数
     */
    private Integer failCount;

    /**
     * 最近一次失败时间
     */
    private LocalDateTime lastFailTime;

    /**
     * 锁定截止时间
     */
    private LocalDateTime lockedUntil;

    /**
     * 租户ID
     */
    private Long tenantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
