package com.payment.enums;

/**
 * 退款申请状态枚举。
 */
public enum RefundApplicationStatus {
    /** 待审核 */
    PENDING,
    /** 已同意 */
    APPROVED,
    /** 已拒绝 */
    REJECTED,
    /** 处理中（已发起渠道退款） */
    PROCESSING,
    /** 已完成（退款到账） */
    COMPLETED,
    /** 退款失败（渠道退款或交付撤销失败） */
    FAILED,
    /** 已取消（用户主动取消） */
    CANCELLED
}
