package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 统一钱包流水实体
 * <p>对应数据库表 unified_wallet_log，记录统一钱包账户每一笔余额变动的明细流水。
 * 每次充值、消费、退款等操作都会生成一条流水记录，用于资金对账和用户账单查询。</p>
 */
@Data
@TableName("unified_wallet_log")
public class UnifiedWalletLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 平台用户 ID，关联 platform_user 表
     */
    private Long platformUserId;

    /**
     * 业务类型，如 RECHARGE-充值、CONSUME-消费、REFUND-退款、WITHDRAW-提现
     */
    private String bizType;

    /**
     * 业务单号，关联具体的业务订单编号（如订单号、退款单号等）
     */
    private String bizNo;

    /**
     * 变动金额（元），正数表示入账，负数表示扣款
     */
    private BigDecimal changeAmount;

    /**
     * 变动前余额（元），记录本次操作之前的账户可用余额
     */
    private BigDecimal balanceBefore;

    /**
     * 变动后余额（元），记录本次操作之后的账户可用余额
     */
    private BigDecimal balanceAfter;

    /**
     * 备注说明，描述本次变动的具体原因
     */
    private String remark;

    /**
     * 创建时间，即流水发生时间
     */
    private LocalDateTime createTime;
}
