package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户端优惠券生命周期事件。
 */
@Data
public class AppCouponTimelineEventVO {
    /** 事件类型：RECEIVE / LOCK / RELEASE / WRITE_OFF / EXPIRE */
    private String eventType;
    /** 用户可读标题 */
    private String title;
    /** 事件说明 */
    private String description;
    /** 事件发生时间 */
    private LocalDateTime occurredAt;
    /** 关联订单号 */
    private String orderNo;
    /** 业务流水号 */
    private String bizNo;
    /** 事件金额，如核销优惠金额 */
    private BigDecimal amount;
    /** 原始状态或原因 */
    private String status;
}
