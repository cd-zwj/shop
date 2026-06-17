package com.payment.service;

import com.payment.entity.CompensationTask;
import com.payment.entity.PaymentBill;
import com.payment.entity.RefundApplication;
import com.payment.entity.RefundReconcileTask;
import com.payment.enums.PaymentStatusReasonEnum;

public interface RefundService {
    void prepareLateCallbackRefund(PaymentBill paymentBill, PaymentStatusReasonEnum statusReason);

    void prepareMerchantApprovedRefund(RefundApplication application);

    void processLateCallbackRefundTask(CompensationTask compensationTask);

    void processRefundReconcileTask(RefundReconcileTask reconcileTask);
}
