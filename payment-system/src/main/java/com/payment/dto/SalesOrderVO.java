package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单视图对象（V1 App / Merchant 接口）
 */
@Data
public class SalesOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private Long tenantId;
    private Long platformUserId;
    private String orderStatus;
    private String payStatus;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal walletDeductAmount;
    private BigDecimal pointsDeductAmount;
    private BigDecimal unifiedWalletDeductAmount;
    private BigDecimal merchantWalletDeductAmount;
    private BigDecimal externalPayAmount;
    private BigDecimal payableAmount;
    private String subject;
    private String source;
    private String walletStrategy;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
