package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付账单视图对象，用于返回支付账单的完整信息（V1 App / Open 接口）。
 */
@Data
public class PaymentBillVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 账单 ID */
    private Long id;
    /** 账单编号（业务唯一标识） */
    private String billNo;
    /** 业务类型（如 ORDER, RECHARGE, WITHDRAWAL） */
    private String bizType;
    /** 关联业务单号 */
    private String bizNo;
    /** 商户租户 ID */
    private Long tenantId;
    /** 用户 ID */
    private Long platformUserId;
    /** 支付渠道编码（如 WECHAT_PAY, ALIPAY） */
    private String channelCode;
    /** 渠道模式（如 JSAPI, NATIVE, H5） */
    private String channelMode;
    /** 支付金额 */
    private BigDecimal payAmount;
    /** 支付状态（如 UNPAID, PAID, CLOSED） */
    private String payStatus;
    /** 第三方交易号 */
    private String thirdPartyBillNo;
    /** 回调处理状态 */
    private String callbackStatus;
    /** 状态备注 */
    private String statusRemark;
    /** 账单过期时间 */
    private LocalDateTime expireTime;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
