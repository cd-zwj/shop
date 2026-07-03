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

/**
 * 退款任务定时调度器。
 * <p>
 * 包含两个定时任务：
 * <ul>
 *   <li>延迟回调退款补偿：处理因支付渠道回调延迟导致的退款状态不同步问题，
 *       以及商家审核通过后需要继续推进的退款任务</li>
 *   <li>退款对账任务：定时拉取待处理和处理中的退款对账任务，
 *       通过 {@link RefundService} 执行对账逻辑</li>
 * </ul>
 * 两个任务的调度间隔均可通过 application.yml 配置，默认均为 60 秒。
 * </p>
 *
 * @see RefundService
 * @see CompensationTask
 * @see RefundReconcileTask
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundTaskScheduler {

    private final CompensationTaskMapper compensationTaskMapper;
    private final RefundReconcileTaskMapper refundReconcileTaskMapper;
    private final RefundService refundService;

    /**
     * 处理延迟回调退款补偿任务。
     * <p>
     * 定时查询状态为 PENDING 或 PROCESSING 的延迟回调退款和商家审核通过退款的补偿任务，
     * 批量取出最多 20 条交由 {@link RefundService#processLateCallbackRefundTask} 处理。
     * 单条处理失败不影响其他任务的执行。
     */
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

    /**
     * 处理退款对账任务。
     * <p>
     * 定时查询状态为 PENDING 或 PROCESSING 且已到达下次重试时间的退款对账任务，
     * 批量取出最多 20 条交由 {@link RefundService#processRefundReconcileTask} 处理。
     * 支持重试时间控制，避免频繁调用第三方支付接口查询退款状态。
     */
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
