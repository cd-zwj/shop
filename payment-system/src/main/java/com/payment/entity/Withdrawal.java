package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现申请实体
 */
@Data
@TableName("withdrawal")
public class Withdrawal implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户ID（商家）
     */
    private Long tenantId;
    
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
    
    private Integer deleted;
    
    private LocalDateTime createTime;
}
