package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台管理端支付账单视图对象，展示全平台支付流水信息。
 */
@Data
public class AdminPaymentBillVO {

    /** 账单ID */
    private Long id;

    /** 账单编号 */
    private String billNo;

    /** 业务类型（ORDER / RECHARGE 等） */
    private String bizType;

    /** 关联业务单号 */
    private String bizNo;

    /** 所属租户（商户）ID */
    private Long tenantId;

    /** 支付用户ID */
    private Long platformUserId;

    /** 支付渠道编码（WECHAT / ALIPAY 等） */
    private String channelCode;

    /** 支付状态 */
    private String payStatus;

    /** 支付金额 */
    private BigDecimal payAmount;

    /** 回调处理状态 */
    private String callbackStatus;

    /** 第三方交易流水号 */
    private String thirdPartyBillNo;

    /** 创建时间 */
    private LocalDateTime createTime;
}
