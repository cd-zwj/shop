package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后退款申请实体。
 * 对应用户发起的退款/退货退款申请，与已有的 RefundOrder（支付渠道退款执行单）区分。
 */
@Data
@TableName("refund_application")
public class RefundApplication implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退款申请单号，唯一 */
    private String refundNo;

    /** 关联订单号 */
    private String orderNo;

    /** 关联订单项ID，可选——部分退款时用 */
    private Long orderItemId;

    /** 申请用户ID */
    private Long platformUserId;

    /** 商户ID */
    private Long tenantId;

    /** 退款类型：REFUND_ONLY=仅退款，RETURN_REFUND=退货退款 */
    private String refundType;

    /** 退款状态：PENDING/APPROVED/REJECTED/PROCESSING/COMPLETED/FAILED/CANCELLED */
    private String refundStatus;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 退款原因 */
    private String reason;

    /** 详细描述 */
    private String description;

    /** 拒绝原因 */
    private String rejectReason;

    /** 审核人ID */
    private Long adminId;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 当前退款目标的交付状态快照，仅用于接口展示。 */
    @TableField(exist = false)
    private String deliveryStatus;

    /** 当前订单剩余可退金额，仅用于接口展示。 */
    @TableField(exist = false)
    private BigDecimal refundableAmount;

    /** 是否建议商家同意后快速进入渠道退款。 */
    @TableField(exist = false)
    private Boolean quickRefundSuggested;

    /** 商家审核提示文案。 */
    @TableField(exist = false)
    private String refundSuggestion;
}
