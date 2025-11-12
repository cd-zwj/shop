package com.payment.dto;

import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 提现申请DTO
 */
@Data
public class WithdrawalApplyDTO {
    
    /**
     * 提现金额
     */
    @NotNull(message = "提现金额不能为空")
    @DecimalMin(value = "0.01", message = "提现金额必须大于0")
    private BigDecimal amount;
    
    /**
     * 银行名称
     */
    @NotBlank(message = "银行名称不能为空")
    private String bankName;
    
    /**
     * 银行账号
     */
    @NotBlank(message = "银行账号不能为空")
    private String bankAccount;
    
    /**
     * 账户名
     */
    @NotBlank(message = "账户名不能为空")
    private String accountName;
}
