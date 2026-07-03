package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录实体。
 * 对应数据库表 refund_record，记录每一条退款请求与第三方支付渠道的交互明细。
 * <p>一个 {@link RefundOrder} 对应一条 RefundRecord，记录向支付渠道发送退款请求的时间、
 * 渠道返回的状态以及第三方退款单号等信息，用于退款追踪和对账。
 */
@Data
@TableName("refund_record")
public class RefundRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退款单号，关联 RefundOrder 的 refundNo */
    private String refundNo;

    /** 支付账单号，关联原始支付记录 */
    private String paymentBillNo;

    /** 支付渠道编码（如 WECHAT_PAY、ALIPAY 等） */
    private String channelCode;

    /** 向渠道发起的退款金额，精确到分 */
    private BigDecimal refundAmount;

    /** 第三方支付平台的原始交易单号 */
    private String thirdPartyBillNo;

    /** 第三方支付平台返回的退款单号 */
    private String thirdPartyRefundNo;

    /**
     * 渠道退款状态。
     * 如：PROCESSING=处理中，SUCCESS=退款成功，FAIL=退款失败等
     */
    private String channelStatus;

    /** 渠道回调通知的原始数据（JSON），用于问题排查和审计 */
    private String notifyData;

    /** 向支付渠道发起退款请求的时间 */
    private LocalDateTime requestTime;

    /** 收到支付渠道回调通知的时间 */
    private LocalDateTime notifyTime;

    /** 退款在渠道侧确认成功的时间 */
    private LocalDateTime successTime;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;
}
