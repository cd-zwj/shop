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
 * 对应数据库表 refund_application，记录用户发起的退款/退货退款申请信息。
 * <p>退款流程：用户申请 → 商户审核(APPROVED/REJECTED) → 系统处理退款(PROCESSING) → 退款到账(COMPLETED)。
 * <p>与 {@link RefundOrder}（支付渠道退款执行单）区分：本实体面向业务侧，RefundOrder 面向支付渠道侧。
 */
@Data
@TableName("refund_application")
public class RefundApplication implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退款申请单号，全局唯一，用于业务标识 */
    private String refundNo;

    /** 关联的原始订单号 */
    private String orderNo;

    /** 关联订单项ID，部分退款时指定具体退款的商品项 */
    private Long orderItemId;

    /** 申请退款的用户ID（平台用户表主键） */
    private Long platformUserId;

    /** 商户ID（租户ID），用于多租户隔离 */
    private Long tenantId;

    /**
     * 退款类型。
     * REFUND_ONLY=仅退款（不退货），RETURN_REFUND=退货退款（需退回商品）
     */
    private String refundType;

    /**
     * 退款状态。
     * PENDING=待审核，APPROVED=已通过，REJECTED=已拒绝，PROCESSING=退款处理中，
     * COMPLETED=已完成，FAILED=退款失败，CANCELLED=用户已取消
     */
    private String refundStatus;

    /** 退款金额，精确到分 */
    private BigDecimal refundAmount;

    /** 用户填写的退款原因 */
    private String reason;

    /** 用户填写的退款详细描述 */
    private String description;

    /** 商户审核时填写的拒绝原因（审核通过时为空） */
    private String rejectReason;

    /** 审核人ID（商户操作员或平台管理员） */
    private Long adminId;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 退款完成时间 */
    private LocalDateTime completeTime;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;

    /**
     * 当前退款目标的交付状态快照，仅用于接口展示。
     * <p>非数据库持久化字段，由业务层在查询时填充。
     */
    @TableField(exist = false)
    private String deliveryStatus;

    /**
     * 当前订单剩余可退金额，仅用于接口展示。
     * <p>非数据库持久化字段，由业务层在查询时计算填充。
     */
    @TableField(exist = false)
    private BigDecimal refundableAmount;

    /**
     * 是否建议商家同意后快速进入渠道退款，仅用于接口展示。
     * <p>非数据库持久化字段，由业务层根据规则判断填充。
     */
    @TableField(exist = false)
    private Boolean quickRefundSuggested;

    /**
     * 商家审核提示文案，仅用于接口展示。
     * <p>非数据库持久化字段，由业务层根据退款类型和交付状态生成。
     */
    @TableField(exist = false)
    private String refundSuggestion;
}
