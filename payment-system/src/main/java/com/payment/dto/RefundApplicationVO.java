package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款申请视图对象，用于 C 端用户和商户端查询退款申请的详细信息。
 */
@Data
public class RefundApplicationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 退款申请 ID */
    private Long id;

    /** 退款单号 */
    private String refundNo;

    /** 关联订单编号 */
    private String orderNo;

    /** 关联订单项 ID（部分退款时使用） */
    private Long orderItemId;

    /** 申请退款的用户 ID */
    private Long platformUserId;

    /** 所属租户（商户）ID */
    private Long tenantId;

    /** 退款类型（REFUND_ONLY / RETURN_REFUND） */
    private String refundType;

    /** 退款状态（PENDING / APPROVED / REJECTED / COMPLETED / FAILED） */
    private String refundStatus;

    /** 退款金额（元） */
    private BigDecimal refundAmount;

    /** 退款原因 */
    private String reason;

    /** 详细描述 */
    private String description;

    /** 拒绝原因（被驳回时填充） */
    private String rejectReason;

    /** 审核人 ID */
    private Long adminId;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 退款完成时间 */
    private LocalDateTime completeTime;

    /** 申请时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
