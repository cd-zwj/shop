package com.payment.enums;

/**
 * 退款对账任务状态枚举。
 *
 * 描述退款对账（核对渠道退款状态与系统记录是否一致）任务的执行状态。
 */
public enum RefundReconcileTaskStatusEnum {
    /** 待处理：对账任务已创建，等待执行 */
    PENDING,
    /** 处理中：对账任务正在执行 */
    PROCESSING,
    /** 处理成功：对账完成且结果一致 */
    SUCCESS,
    /** 处理失败：对账过程中发生异常或结果不一致 */
    FAIL,
    /** 已取消：对账任务被取消 */
    CANCELLED
}
