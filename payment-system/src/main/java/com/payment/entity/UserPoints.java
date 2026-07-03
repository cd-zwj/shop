package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户积分实体，对应数据库表 user_points。
 * <p>记录 C 端用户在某一租户下的积分余额及累计收支情况，使用乐观锁保障并发安全。</p>
 *
 * @deprecated 已废弃，请使用 {@code MemberPointsAccount} 替代，该实体提供更完善的会员积分账户功能。
 */
@Data
@TableName("user_points")
@Deprecated
public class UserPoints implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 当前积分余额
     */
    private Integer points;

    /**
     * 累计获得积分总数
     */
    private Integer totalEarned;

    /**
     * 累计使用/消耗积分总数
     */
    private Integer totalUsed;

    /** 软删除标记：0-正常，1-已删除 */
    private Integer deleted;

    /** 乐观锁版本号，每次更新自动递增，防止并发余额扣减冲突 */
    @Version
    private Integer version;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;
}
