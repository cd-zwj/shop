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
     * 账号类型：USERNAME-用户名，PHONE-手机号，AUTH_KEY-第三方认证Key
     */
    private String accountType;

    /**
     * 账号值
     */
    private String accountValue;

    /**
     * 连续失败次数
     */
    private Integer failCount;

    /**
     * 最近一次失败时间
     */
    private LocalDateTime lastFailTime;

    /**
     * 锁定开始时间
     */
    private LocalDateTime lockStartTime;

    /**
     * 锁定结束时间
     */
    private LocalDateTime lockEndTime;

    /**
     * 锁定状态：UNLOCKED-未锁定，LOCKED-已锁定
     */
    private String lockStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
