package com.payment.dto;

import lombok.Data;

/**
 * 退款处理结果基类，封装退款渠道返回的通用信息。
 */
@Data
public class RefundResultDTO {

    /** 是否处理成功 */
    private boolean success;

    /** 渠道退款状态 */
    private String channelStatus;

    /** 第三方退款流水号 */
    private String providerRefundNo;

    /** 渠道原始状态码 */
    private String rawStatus;

    /** 处理结果消息 */
    private String message;
}
