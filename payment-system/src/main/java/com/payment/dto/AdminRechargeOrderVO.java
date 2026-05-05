package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminRechargeOrderVO {

    private Long id;
    private String rechargeNo;
    private String walletType;
    private Long tenantId;
    private Long platformUserId;
    private BigDecimal rechargeAmount;
    private BigDecimal giftAmount;
    private Integer giftPoints;
    private BigDecimal actualCreditAmount;
    private String bizStatus;
    private LocalDateTime createTime;
}
