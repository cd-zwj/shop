package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款渠道回调请求参数，由支付网关回调时传入。
 */
@Data
public class RefundRequestDTO {

    /** 退款单号（平台内部生成） */
    private String refundNo;

    /** 退款金额（元） */
    private BigDecimal refundAmount;

    /** 退款原因 */
    private String refundReason;
}
