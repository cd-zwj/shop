package com.payment.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 提现审核DTO
 */
@Data
public class WithdrawalApproveDTO {
    
    /**
     * 提现申请ID
     */
    @NotNull(message = "提现申请ID不能为空")
    private Long withdrawalId;
    
    /**
     * 是否通过（true-通过，false-拒绝）
     */
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;
    
    /**
     * 拒绝原因（拒绝时必填）
     */
    private String rejectReason;
}
