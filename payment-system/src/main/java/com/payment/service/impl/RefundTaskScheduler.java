package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.constant.RefundConstants;
import com.payment.entity.CompensationTask;
import com.payment.entity.RefundReconcileTask;
import com.payment.enums.RefundReconcileTaskStatusEnum;
import com.payment.mapper.CompensationTaskMapper;
import com.payment.mapper.RefundReconcileTaskMapper;
import com.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundTaskScheduler {

    private final CompensationTaskMapper compensationTaskMapper;
    private final RefundReconcileTaskMapper refundReconcileTaskMapper;
    private final RefundService refundService;

    @Scheduled(fixedDelayString = "${payment.refund.compensation.fixed-delay-ms:60000}")
    public void processLateCallbackRefundTasks() {
        List<CompensationTask> tasks = compensationTaskMapper.selectList(new LambdaQueryWrapper<CompensationTask>()
                .in(CompensationTask::getBizType,
                        RefundConstants.LATE_CALLBACK_REFUND_BIZ_TYPE,
                        RefundConstants.MERCHANT_APPROVED_REFUND_BIZ_TYPE)
                .in(CompensationTask::getTaskStatus, "PENDING", "PROCESSING")
                .orderByAsc(CompensationTask::getUpdateTime)
                .last("limit 20"));
        for (CompensationTask task : tasks) {
            try {
                refundService.processLateCallbackRefundTask(task);
            } catch (Exception e) {
                log.error("Failed to process late callback refund task: {}", task.getTaskNo(), e);
            }
        }
    }

    @Scheduled(fixedDelayString = "${payment.refund.reconcile.fixed-delay-ms:60000}")
    public void processRefundReconcileTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<RefundReconcileTask> tasks = refundReconcileTaskMapper.selectList(new LambdaQueryWrapper<RefundReconcileTask>()
                .in(RefundReconcileTask::getTaskStatus,
                        RefundReconcileTaskStatusEnum.PENDING.name(),
                        RefundReconcileTaskStatusEnum.PROCESSING.name())
                .and(wrapper -> wrapper.isNull(RefundReconcileTask::getNextRetryTime)
                        .or()
                        .le(RefundReconcileTask::getNextRetryTime, now))
                .orderByAsc(RefundReconcileTask::getNextRetryTime)
                .last("limit 20"));
        for (RefundReconcileTask task : tasks) {
            try {
                refundService.processRefundReconcileTask(task);
            } catch (Exception e) {
                log.error("Failed to process refund reconcile task: {}", task.getTaskNo(), e);
            }
        }
    }
}
