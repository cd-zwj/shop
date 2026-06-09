package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付账单视图对象（V1 App / Open 接口）
 */
@Data
public class PaymentBillVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String billNo;
    private String bizType;
    private String bizNo;
    private Long tenantId;
    private Long platformUserId;
    private String channelCode;
    private String channelMode;
    private BigDecimal payAmount;
    private String payStatus;
    private String thirdPartyBillNo;
    private String callbackStatus;
    private String statusRemark;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
