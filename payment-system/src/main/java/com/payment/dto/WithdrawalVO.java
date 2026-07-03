package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现记录视图对象，包含提现申请的完整信息及关联商家信息。
 */
@Data
public class WithdrawalVO {

    /** 提现申请 ID */
    private Long id;

    /** 所属租户（商户）ID */
    private Long tenantId;

    /** 商家名称 */
    private String merchantName;

    /** 提现金额（元） */
    private BigDecimal amount;

    /** 收款银行名称 */
    private String bankName;

    /** 收款银行账号 */
    private String bankAccount;

    /** 收款账户名 */
    private String accountName;

    /** 提现状态（0-待审核，1-已通过，2-已拒绝） */
    private Integer status;

    /** 拒绝原因（被驳回时填充） */
    private String rejectReason;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 审核时间 */
    private LocalDateTime approveTime;

    /** 审核人 ID */
    private Long approverId;

    /** 审核人姓名 */
    private String approverName;

    /** 创建时间 */
    private LocalDateTime createTime;
}
