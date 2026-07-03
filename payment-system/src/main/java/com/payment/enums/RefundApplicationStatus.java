package com.payment.enums;

/**
 * 退款申请状态枚举。
 *
 * 描述退款申请单的完整生命周期，
 * 涵盖商户审核、渠道退款、退款完成等各阶段。
 */
public enum RefundApplicationStatus {
    /** 待审核：退款申请已提交，等待商户审核 */
    PENDING,
    /** 已同意：商户审核通过，准备发起渠道退款 */
    APPROVED,
    /** 已拒绝：商户审核拒绝，退款流程终止 */
    REJECTED,
    /** 处理中：已发起渠道退款，等待第三方支付渠道处理 */
    PROCESSING,
    /** 已完成：退款到账，退款流程结束 */
    COMPLETED,
    /** 退款失败：渠道退款或交付撤销失败，需人工介入 */
    FAILED,
    /** 已取消：用户主动取消退款申请 */
    CANCELLED
}
