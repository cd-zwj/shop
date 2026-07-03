package com.payment.enums;

/**
 * 退款审核状态枚举。
 *
 * 描述商户对退款申请的审核结果。
 */
public enum RefundAuditStatusEnum {
    /** 待审核：退款申请已提交，等待商户处理 */
    PENDING,
    /** 审核通过：商户同意退款 */
    APPROVED,
    /** 审核拒绝：商户拒绝退款 */
    REJECTED
}
