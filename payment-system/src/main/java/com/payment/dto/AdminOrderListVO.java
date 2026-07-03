package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台管理端订单列表视图对象，用于展示全平台订单概要信息。
 */
@Data
public class AdminOrderListVO {

    /** 订单ID */
    private Long id;

    /** 订单编号 */
    private String orderNo;

    /** 所属租户（商户）ID */
    private Long tenantId;

    /** 下单用户ID */
    private Long platformUserId;

    /** 订单主题/摘要 */
    private String subject;

    /** 订单状态 */
    private String orderStatus;

    /** 支付状态 */
    private String payStatus;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 外部支付金额（通过第三方支付渠道实际支付的金额） */
    private BigDecimal externalPayAmount;

    /** 下单时间 */
    private LocalDateTime createTime;
}
