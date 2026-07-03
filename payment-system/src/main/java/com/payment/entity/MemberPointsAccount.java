package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员积分账户实体，对应数据库表 member_points_account。
 * 记录平台用户的积分余额、累计获得/使用积分等信息，是积分体系的核心账户。
 * 采用乐观锁（version）保障并发场景下积分余额的安全性。
 */
@Data
@TableName("member_points_account")
public class MemberPointsAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID，多租户隔离标识 */
    private Long tenantId;

    /** 平台用户ID，关联 platform_user 表 */
    private Long platformUserId;

    /** 当前可用积分余额 */
    private Integer points;

    /** 累计获得的积分总数 */
    private Integer totalEarned;

    /** 累计使用的积分总数 */
    private Integer totalUsed;

    /** 乐观锁版本号，用于并发控制 */
    @Version
    private Integer version;

    /** 账户状态：1-正常，0-禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
