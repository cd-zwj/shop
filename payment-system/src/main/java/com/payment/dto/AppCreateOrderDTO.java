package com.payment.dto;

import com.payment.enums.WalletStrategyEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AppCreateOrderDTO {
    @NotNull(message = "商户ID不能为空")
    private Long tenantId;

    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于0")
    private BigDecimal totalAmount;

    @NotBlank(message = "订单标题不能为空")
    private String subject;

    private String source;

    @NotNull(message = "钱包策略不能为空")
    private WalletStrategyEnum walletStrategy;

    private BigDecimal unifiedWalletAmount;
    private BigDecimal merchantWalletAmount;
    private Boolean allowExternalPayFallback;
}
