package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单实体。
 * 对应数据库表 refund_order，记录退款的支付渠道执行信息。
 * <p>与 {@link RefundApplication}（业务侧退款申请）区分：本实体面向支付渠道侧，
 * 负责实际调用微信/支付宝等渠道完成退款资金回转。
 * <p>支持拆单退款：一笔退款申请可能拆分为钱包退款 + 外部渠道退款两部分。
 */
@Data
@TableName("refund_order")
public class RefundOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退款单号，全局唯一，关联 RefundApplication 的 refundNo */
    private String refundNo;

    /** 业务类型，标识退款来源场景（如 ORDER_REFUND 等） */
    private String bizType;

    /** 业务编号，关联具体的业务单据号 */
    private String bizNo;

    /** 商户ID（租户ID），用于多租户隔离 */
    private Long tenantId;

    /** 发起退款的平台用户ID */
    private Long platformUserId;

    /** 关联的原始支付订单号 */
    private String orderNo;

    /** 关联的支付账单号（payment_bill 表），定位原始支付记录 */
    private String paymentBillNo;

    /** 支付渠道编码（如 WECHAT_PAY、ALIPAY 等） */
    private String channelCode;

    /** 退款原因 */
    private String refundReason;

    /** 申请退款金额，用户发起退款时填写的金额 */
    private BigDecimal applyAmount;

    /** 实际退款金额（总），可能与申请金额不同（如部分退款） */
    private BigDecimal refundAmount;

    /** 钱包退款金额，退回到用户钱包的部分 */
    private BigDecimal walletRefundAmount;

    /** 外部渠道退款金额，通过第三方支付渠道原路退回的部分 */
    private BigDecimal externalRefundAmount;

    /**
     * 退款状态。
     * 如：PENDING=待退款，PROCESSING=处理中，SUCCESS=退款成功，FAILED=退款失败
     */
    private String refundStatus;

    /**
     * 审核状态。
     * 如：PENDING=待审核，APPROVED=已通过，REJECTED=已拒绝
     */
    private String auditStatus;

    /** 审核人ID */
    private Long auditBy;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 退款成功时间 */
    private LocalDateTime successTime;

    /** 退款失败原因 */
    private String failReason;

    /** 逻辑删除标志：0=未删除，1=已删除 */
    private Integer deleted;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;
}
