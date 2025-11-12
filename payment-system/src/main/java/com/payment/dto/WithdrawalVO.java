package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现申请VO（包含商家信息）
 */
@Data
public class WithdrawalVO {
    
    private Long id;
    
    /**
     * 租户ID（商家）
     */
    private Long tenantId;
    
    /**
     * 商家名称
     */
    private String merchantName;
    
    /**
     * 提现金额
     */
    private BigDecimal amount;
    
    /**
     * 银行名称
     */
    private String bankName;
    
    /**
     * 银行账号
     */
    private String bankAccount;
    
    /**
     * 账户名
     */
    private String accountName;
    
    /**
     * 状态（0-待审核，1-已通过，2-已拒绝）
     */
    private Integer status;
    
    /**
     * 拒绝原因
     */
    private String rejectReason;
    
    /**
     * 申请时间
     */
    private LocalDateTime applyTime;
    
    /**
     * 审核时间
     */
    private LocalDateTime approveTime;
    
    /**
     * 审核人ID
     */
    private Long approverId;
    
    /**
     * 审核人姓名
     */
    private String approverName;
    
    private LocalDateTime createTime;
}
