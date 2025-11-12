package com.payment.dto;

import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 充值规则DTO
 */
@Data
public class RechargeRuleDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    
    /**
     * 充值金额
     */
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    private BigDecimal rechargeAmount;
    
    /**
     * 赠送金额
     */
    @NotNull(message = "赠送金额不能为空")
    @DecimalMin(value = "0", message = "赠送金额不能为负数")
    private BigDecimal bonusAmount;
    
    /**
     * 是否启用（0-否，1-是）
     */
    private Integer enabled;
    
    /**
     * 排序
     */
    private Integer sortOrder;
}
