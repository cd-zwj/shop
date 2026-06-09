package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款申请视图对象（V1 App / Merchant 接口）
 */
@Data
public class RefundApplicationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String refundNo;
    private String orderNo;
    private Long orderItemId;
    private Long platformUserId;
    private Long tenantId;
    private String refundType;
    private String refundStatus;
    private BigDecimal refundAmount;
    private String reason;
    private String description;
    private String rejectReason;
    private Long adminId;
    private LocalDateTime auditTime;
    private LocalDateTime completeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
