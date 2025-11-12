package com.payment.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 创建订单DTO
 */
@Data
public class CreateOrderDTO {
    @NotNull(message = "订单金额不能为空")
    private BigDecimal amount;
    
    @NotBlank(message = "支付方式不能为空")
    private String payType; // WECHAT 或 ALIPAY 或 BALANCE 或 MIXED
    
    @NotBlank(message = "订单标题不能为空")
    private String subject;
    
    private String body;
    
    private String notifyUrl;
    
    /**
     * 使用余额支付的金额（仅当payType为BALANCE或MIXED时有效）
     */
    private BigDecimal balanceAmount;
    
    /**
     * 是否使用余额支付
     */
    private Boolean useBalance;
}

