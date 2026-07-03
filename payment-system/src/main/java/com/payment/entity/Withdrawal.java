package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现申请实体。
 * 对应数据库表 withdrawal，记录商户向平台发起的资金提现申请。
 * <p>提现流程：商户发起申请 → 平台审核(通过/拒绝) → 资金转账到银行卡。
 * <p>提现来源为商户钱包（merchant_wallet_account）余额。
 */
@Data
@TableName("withdrawal")
public class Withdrawal implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID（商户），用于多租户隔离 */
    private Long tenantId;

    /** 提现金额，精确到分 */
    private BigDecimal amount;

    /** 收款银行名称 */
    private String bankName;

    /** 收款银行账号 */
    private String bankAccount;

    /** 收款账户名称（与银行账号匹配） */
    private String accountName;

    /**
     * 提现状态。
     * 0=待审核，1=已通过（打款中/已打款），2=已拒绝
     */
    private Integer status;

    /** 审核拒绝原因（审核通过时为空） */
    private String rejectReason;

    /** 商户提交提现申请的时间 */
    private LocalDateTime applyTime;

    /** 平台审核时间 */
    private LocalDateTime approveTime;

    /** 审核人ID（平台管理员） */
    private Long approverId;

    /** 逻辑删除标志：0=未删除，1=已删除 */
    private Integer deleted;

    /** 记录创建时间 */
    private LocalDateTime createTime;
}
