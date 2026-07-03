package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录失败记录实体，对应数据库表 login_fail_record。
 * <p>用于实现登录防暴力破解机制，记录各账号的连续失败次数和锁定状态。
 * 当连续失败次数达到阈值时自动锁定账号一段时间。</p>
 */
@Data
@TableName("login_fail_record")
public class LoginFailRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账号类型：USERNAME-用户名，PHONE-手机号，AUTH_KEY-第三方认证Key
     */
    private String accountType;

    /**
     * 账号值（与 accountType 对应的具体账号标识）
     */
    private String accountValue;

    /**
     * 连续登录失败次数，登录成功后重置为 0
     */
    private Integer failCount;

    /**
     * 最近一次登录失败的时间
     */
    private LocalDateTime lastFailTime;

    /**
     * 锁定开始时间，未锁定时为 null
     */
    private LocalDateTime lockStartTime;

    /**
     * 锁定结束时间，到达此时间后账号自动解锁
     */
    private LocalDateTime lockEndTime;

    /**
     * 锁定状态：UNLOCKED-未锁定，LOCKED-已锁定
     */
    private String lockStatus;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;
}
