package com.payment.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 提现审核请求参数，由平台管理员审批提现申请时传入。
 */
@Data
public class WithdrawalApproveDTO {

    /** 提现申请 ID */
    @NotNull(message = "提现申请ID不能为空")
    private Long withdrawalId;

    /** 审核结果（true-通过，false-拒绝） */
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    /** 拒绝原因（拒绝时必填） */
    private String rejectReason;
}
