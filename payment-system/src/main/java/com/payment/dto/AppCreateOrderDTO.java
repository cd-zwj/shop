package com.payment.dto;

import com.payment.enums.PaymentChannelCodeEnum;
import com.payment.enums.WalletStrategyEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AppCreateOrderDTO {

    @NotNull(message = "Tenant id is required")
    private Long tenantId;

    @DecimalMin(value = "0.01", message = "Order amount must be greater than 0")
    private BigDecimal totalAmount;

    private String subject;

    private String source;

    @Valid
    @NotEmpty(message = "Order items are required")
    private List<AppCreateOrderItemDTO> items;

    @NotNull(message = "Wallet strategy is required")
    private WalletStrategyEnum walletStrategy;

    private PaymentChannelCodeEnum paymentChannelCode;

    private BigDecimal unifiedWalletAmount;
    private BigDecimal merchantWalletAmount;
    private Boolean allowExternalPayFallback;
    private Long selectedUserCouponId;
    private Integer usedPoints;
    private Integer requestedPoints;
}
