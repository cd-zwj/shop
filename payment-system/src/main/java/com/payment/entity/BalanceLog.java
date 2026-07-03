package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 余额变动日志实体
 * <p>对应数据库表 balance_log，记录商户余额每一次变动的明细日志。
 * 涵盖充值、消费、退款、提现等所有影响余额的操作，用于资金对账和审计追溯。</p>
 */
@Data
@TableName("balance_log")
public class BalanceLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户 ID，标识所属商户，用于多租户数据隔离
     */
    private Long tenantId;

    /**
     * 用户 ID，关联触发本次余额变动的用户
     */
    private Long userId;

    /**
     * 变动类型：RECHARGE-充值，CONSUME-消费，REFUND-退款，WITHDRAW-提现
     */
    private String changeType;

    /**
     * 变动金额（元），正数表示入账，负数表示扣款
     */
    private BigDecimal changeAmount;

    /**
     * 变动前余额（元），记录本次操作之前的账户余额
     */
    private BigDecimal balanceBefore;

    /**
     * 变动后余额（元），记录本次操作之后的账户余额
     */
    private BigDecimal balanceAfter;

    /**
     * 关联订单号，指向触发本次变动的业务订单编号
     */
    private String orderNo;

    /**
     * 备注说明，描述本次变动的具体原因
     */
    private String remark;

    /**
     * 创建时间，即日志发生时间
     */
    private LocalDateTime createTime;
}
