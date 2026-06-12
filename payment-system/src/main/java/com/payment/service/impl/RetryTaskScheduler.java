package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.entity.CompensationTask;
import com.payment.entity.RetryTask;
import com.payment.mapper.CompensationTaskMapper;
import com.payment.mapper.RetryTaskMapper;
import com.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 重试任务调度器。
 * 轮询 retry_task 表中 PENDING 且 (nextRetryTime IS NULL OR nextRetryTime <= now) 的记录。
 *
 * 已接入的处理器：
 * - REFUND_QUERY → 委托 RefundService.processLateCallbackRefundTask
 *
 * 未接入的 taskType（任务保持 PENDING + 退避重试，不标记 DEAD）：
 * - PAYMENT_CALLBACK, ORDER_CLOSE, RECHARGE_CREDIT, COUPON_COMPENSATE, SMS_RETRY
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryTaskScheduler {

    private static final int BATCH_SIZE = 20;

    private final RetryTaskMapper retryTaskMapper;
    private final CompensationTaskMapper compensationTaskMapper;
    private final RefundService refundService;

    @Scheduled(fixedDelayString = "${payment.retry-task.fixed-delay-ms:60000}")
    public void processRetryTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<RetryTask> tasks = retryTaskMapper.selectList(new LambdaQueryWrapper<RetryTask>()
                .eq(RetryTask::getTaskStatus, "PENDING")
                .and(w -> w.isNull(RetryTask::getNextRetryTime).or().le(RetryTask::getNextRetryTime, now))
                .orderByAsc(RetryTask::getCreateTime)
                .last("LIMIT " + BATCH_SIZE));

        for (RetryTask task : tasks) {
            try {
                dispatch(task);
            } catch (Exception e) {
                log.error("重试任务执行失败: taskNo={}, taskType={}", task.getTaskNo(), task.getTaskType(), e);
                onFailure(task, e.getMessage());
            }
        }
    }

    private void dispatch(RetryTask task) {
        switch (task.getTaskType()) {
            case "REFUND_QUERY" -> handleRefundQuery(task);
            default -> handleUnsupported(task);
        }
    }

    /**
     * REFUND_QUERY: 查找对应的 CompensationTask 委托给已有的退款对账流程。
     * 如果 CompensationTask 已不存在或已终态，直接标记 RetryTask 完成/死亡。
     */
    private void handleRefundQuery(RetryTask task) {
        CompensationTask ct = compensationTaskMapper.selectOne(
                new LambdaQueryWrapper<CompensationTask>()
                        .eq(CompensationTask::getBizNo, task.getBizNo())
                        .eq(CompensationTask::getBizType, "LATE_CALLBACK_REFUND"));

        if (ct == null) {
            // 对应补偿任务不存在（可能已被清理），标记死亡
            markDead(task, "关联的 CompensationTask 不存在: bizNo=" + task.getBizNo());
            return;
        }
        if ("SUCCESS".equals(ct.getTaskStatus())) {
            onSuccess(task, "退款补偿任务已完成");
            return;
        }
        if ("FAIL".equals(ct.getTaskStatus())) {
            markDead(task, "退款补偿任务已失败: " + ct.getRemark());
            return;
        }

        // 委托给已有的退款对账处理
        refundService.processLateCallbackRefundTask(ct);

        // 同步结果
        if ("SUCCESS".equals(ct.getTaskStatus())) {
            onSuccess(task, ct.getRemark());
        } else if ("FAIL".equals(ct.getTaskStatus())) {
            markDead(task, "退款处理失败: " + ct.getRemark());
        } else {
            // PROCESSING 或 PENDING：退款仍在进行中，稍后重试
            onFailure(task, "退款处理中，等待下次重试");
        }
    }

    /**
     * 未接入的 taskType：不标记 DEAD，使用退避重试保留机会。
     * 超过 maxRetryCount 后自然转为 DEAD。
     */
    private void handleUnsupported(RetryTask task) {
        log.warn("重试任务暂无处理器，退避重试: taskNo={}, taskType={}", task.getTaskNo(), task.getTaskType());
        onFailure(task, "暂无对应处理器: " + task.getTaskType());
    }

    private void onSuccess(RetryTask task, String message) {
        task.setTaskStatus("SUCCESS");
        task.setLastErrorMessage(null);
        task.setUpdateTime(LocalDateTime.now());
        retryTaskMapper.updateById(task);
        log.info("重试任务成功: taskNo={}", task.getTaskNo());
    }

    private void onFailure(RetryTask task, String message) {
        int count = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;
        task.setRetryCount(count);
        task.setLastErrorMessage(message);

        if (count >= (task.getMaxRetryCount() == null ? 16 : task.getMaxRetryCount())) {
            task.setTaskStatus("DEAD");
            log.warn("重试任务达到最大重试次数，标记为 DEAD: taskNo={}, retryCount={}", task.getTaskNo(), count);
        } else {
            task.setTaskStatus("PENDING");
            task.setNextRetryTime(LocalDateTime.now().plusMinutes(Math.min(count, 5)));
        }
        task.setUpdateTime(LocalDateTime.now());
        retryTaskMapper.updateById(task);
    }

    private void markDead(RetryTask task, String reason) {
        task.setTaskStatus("DEAD");
        task.setLastErrorMessage(reason);
        task.setUpdateTime(LocalDateTime.now());
        retryTaskMapper.updateById(task);
    }
}
