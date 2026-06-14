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

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterRecoveryScheduler {

    private static final int MAX_RETRY_COUNT = 3;
    private static final int BATCH_SIZE = 50;

    private final DeadLetterTaskMapper deadLetterTaskMapper;
    private final RabbitTemplate rabbitTemplate;

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

    private void markDead(DeadLetterTask task, String reason) {
        task.setHandleStatus("DEAD");
        task.setFailReason(reason);
        task.setLastHandleTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        deadLetterTaskMapper.updateById(task);
        log.warn("Dead letter marked DEAD, id={}, reason={}", task.getId(), reason);
    }
}
