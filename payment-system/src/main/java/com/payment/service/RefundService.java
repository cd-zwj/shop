package com.payment.service;

import com.payment.entity.CompensationTask;
import com.payment.entity.PaymentBill;
import com.payment.entity.RefundApplication;
import com.payment.entity.RefundReconcileTask;
import com.payment.enums.PaymentStatusReasonEnum;

/**
 * 退款执行服务接口。
 * <p>
 * 负责与第三方支付渠道交互的退款执行逻辑，包括延迟回调退款准备、
 * 商户审核通过后的退款准备、退款任务处理以及退款对账。
 * 与 {@link RefundApplicationService}（退款申请生命周期管理）区分，
 * 本服务专注于退款的底层执行和渠道交互。
 */
public interface RefundService {

    /**
     * 准备延迟回调退款。
     * <p>
     * 当支付回调延迟到达且业务已关闭时，为已支付的账单准备退款任务。
     *
     * @param paymentBill  支付账单实体
     * @param statusReason 状态变更原因
     */
    void prepareLateCallbackRefund(PaymentBill paymentBill, PaymentStatusReasonEnum statusReason);

    /**
     * 准备商户审核通过后的退款。
     * <p>
     * 商户审核退款申请通过后，创建退款执行任务。
     *
     * @param application 退款申请实体
     */
    void prepareMerchantApprovedRefund(RefundApplication application);

    /**
     * 处理延迟回调退款补偿任务。
     *
     * @param compensationTask 补偿任务实体
     */
    void processLateCallbackRefundTask(CompensationTask compensationTask);

    /**
     * 处理退款对账任务。
     * <p>
     * 定期校验退款状态，确保渠道退款结果与系统记录一致。
     *
     * @param reconcileTask 退款对账任务实体
     */
    void processRefundReconcileTask(RefundReconcileTask reconcileTask);
}
