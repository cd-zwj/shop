package com.payment.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 创建充值订单DTO
 */
@Data
public class CreateRechargeOrderDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 充值规则ID
     */
    @NotNull(message = "充值规则ID不能为空")
    private Long ruleId;
}
