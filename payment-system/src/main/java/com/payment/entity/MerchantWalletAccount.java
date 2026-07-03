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
 * 商户钱包账户实体
 * <p>对应数据库表 merchant_wallet_account，存储商户级别的钱包余额信息。
 * 属于双钱包系统中的"商户钱包"部分，与统一钱包（unified_wallet_account）共同构成双钱包架构。
 * 每个租户下的用户拥有独立的商户钱包，余额操作使用乐观锁 {@code @Version} 防止并发不一致。</p>
 */
@Data
@TableName("merchant_wallet_account")
public class MerchantWalletAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户 ID，标识商户所属的租户，用于多租户数据隔离
     */
    private Long tenantId;

    /**
     * 平台用户 ID，关联 platform_user 表
     */
    private Long platformUserId;

    /**
     * 可用余额（元），商户当前可直接使用的金额
     */
    private BigDecimal availableAmount;

    /**
     * 冻结余额（元），因退款审核、提现申请等原因被临时冻结的金额
     */
    private BigDecimal frozenAmount;

    /**
     * 累计充值总额（元），记录商户历史充值总金额，仅增不减
     */
    private BigDecimal totalRecharge;

    /**
     * 累计消费总额（元），记录商户历史消费总金额，仅增不减
     */
    private BigDecimal totalConsume;

    /**
     * 乐观锁版本号，用于余额并发操作的安全保障，每次更新自动 +1
     */
    @Version
    private Integer version;

    /**
     * 账户状态：0-正常，1-冻结，2-注销
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;
}
