package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.entity.DeadLetterTask;
import com.payment.mapper.DeadLetterTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 死信队列消息恢复调度器。
 * <p>
 * 定时扫描死信任务表中状态为 PENDING 的记录，尝试将消息重新投递到原始交换机和路由键。
 * 每次批量处理 50 条，处理成功标记为 RECOVERED；处理失败则递增重试计数，
 * 超过最大重试次数（3 次）后标记为 DEAD 不再重试。
 * 调度间隔默认 60 秒。
 * </p>
 *
 * @see DeadLetterTask
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterRecoveryScheduler {

    /** 最大重试次数，超过后标记为 DEAD */
    private static final int MAX_RETRY_COUNT = 3;

    /** 每次批量处理的记录数 */
    private static final int BATCH_SIZE = 50;

    private final DeadLetterTaskMapper deadLetterTaskMapper;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 定时恢复待处理的死信消息。
     * <p>
     * 每 60 秒执行一次，查询状态为 PENDING 的死信任务并逐条尝试恢复。
     * 恢复失败时递增重试计数，超过最大次数后标记为 DEAD。
     */
    @Scheduled(fixedDelay = 60000)
    public void recoverPendingDeadLetters() {
        List<DeadLetterTask> tasks = deadLetterTaskMapper.selectList(new LambdaQueryWrapper<DeadLetterTask>()
                .eq(DeadLetterTask::getHandleStatus, "PENDING")
                .orderByAsc(DeadLetterTask::getCreateTime)
                .last("LIMIT " + BATCH_SIZE));

        for (DeadLetterTask task : tasks) {
            try {
                attemptRecover(task);
            } catch (Exception e) {
                log.error("Dead letter recovery failed, id={}, exchange={}, routingKey={}",
                        task.getId(), task.getExchangeName(), task.getRoutingKey(), e);
                incrementRetryCount(task);
            }
        }
    }

    /**
     * 尝试恢复单条死信任务。
     * <p>
     * 校验交换机和路由键不为空后，通过 RabbitTemplate 重新发送消息到原始队列。
     * 发送成功则标记为 RECOVERED，校验失败则直接标记为 DEAD。
     *
     * @param task 死信任务实体
     */
    private void attemptRecover(DeadLetterTask task) {
        String exchange = task.getExchangeName();
        String routingKey = task.getRoutingKey();

        if (exchange == null || exchange.isBlank() || routingKey == null || routingKey.isBlank()) {
            markDead(task, "Missing exchange or routing key for redelivery");
            return;
        }

        rabbitTemplate.convertAndSend(exchange, routingKey, task.getMessageBody());

        task.setHandleStatus("RECOVERED");
        task.setLastHandleTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        deadLetterTaskMapper.updateById(task);
        log.info("Dead letter recovered, id={}, exchange={}, routingKey={}", task.getId(), exchange, routingKey);
    }

    /**
     * 递增死信任务的重试计数，超过最大次数后标记为 DEAD。
     *
     * @param task 死信任务实体
     */
    private void incrementRetryCount(DeadLetterTask task) {
        int currentRetry = task.getRetryCount() == null ? 0 : task.getRetryCount();
        int newRetry = currentRetry + 1;
        task.setRetryCount(newRetry);
        task.setLastHandleTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());

        if (newRetry > MAX_RETRY_COUNT) {
            task.setHandleStatus("DEAD");
            log.warn("Dead letter exceeded max retries, marked DEAD, id={}, retryCount={}", task.getId(), newRetry);
        }

        deadLetterTaskMapper.updateById(task);
    }

    /**
     * 将死信任务标记为 DEAD，记录失败原因。
     *
     * @param task   死信任务实体
     * @param reason 标记为 DEAD 的原因说明
     */
    private void markDead(DeadLetterTask task, String reason) {
        task.setHandleStatus("DEAD");
        task.setFailReason(reason);
        task.setLastHandleTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        deadLetterTaskMapper.updateById(task);
        log.warn("Dead letter marked DEAD, id={}, reason={}", task.getId(), reason);
    }
}
