package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户余额实体，对应数据库表 user_balance。
 * <p>记录用户的钱包余额信息，包括当前余额、累计充值和累计消费。
 * 涉及资金操作时使用乐观锁（{@code @Version}）保障并发安全。</p>
 *
 * @deprecated 已迁移至 UnifiedWalletAccount（统一钱包账户），本实体仅供历史兼容，新代码请使用 UnifiedWalletAccount 替代
 */
@Data
@TableName("user_balance")
@Deprecated
public class UserBalance implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 余额记录主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID，用于多租户行级隔离 */
    private Long tenantId;

    /** 用户ID，关联 sys_user 表，全局唯一 */
    private Long userId;

    /** 当前可用余额，单位为元，精度到分 */
    private BigDecimal balance;

    /** 累计充值总额，单位为元，每次充值时累加 */
    private BigDecimal totalRecharge;

    /** 累计消费总额，单位为元，每次消费时累加 */
    private BigDecimal totalConsume;

    /** 逻辑删除标记：0-未删除，1-已删除 */
    private Integer deleted;

    /** 乐观锁版本号，每次更新余额时自增，防止并发扣款超卖 */
    @Version
    private Integer version;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;
}
