package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.entity.MessageOutbox;
import com.payment.enums.OutboxSendStatusEnum;
import com.payment.mapper.MessageOutboxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Outbox消息重试调度器。定期扫描message_outbox表中发送失败或待发送的记录，
 * 尝试重新投递到RabbitMQ，采用指数退避策略，超过最大重试次数后标记为DEAD。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageOutboxRetryScheduler {

    private static final int MAX_RETRY_COUNT = 16;
    private static final long RETRY_BASE_DELAY_SECONDS = 30;
    private static final int BATCH_SIZE = 100;
    private static final Set<String> RETRYABLE_STATUS = Set.of(
            OutboxSendStatusEnum.PENDING.name(),
            OutboxSendStatusEnum.FAILED.name()
    );

    private final MessageOutboxMapper messageOutboxMapper;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 定时批量扫描待重试的Outbox消息，逐条尝试重新投递。
     * 查询状态为PENDING或FAILED且到达下次重试时间的记录，按创建时间升序取前BATCH_SIZE条。
     */
    @Scheduled(fixedDelayString = "${payment.mq.outbox.retry.fixed-delay-ms:5000}")
    public void retryPendingOutbox() {
        List<MessageOutbox> records = messageOutboxMapper.selectList(new LambdaQueryWrapper<MessageOutbox>()
                .in(MessageOutbox::getSendStatus, RETRYABLE_STATUS)
                .le(MessageOutbox::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(MessageOutbox::getCreateTime)
                .last("LIMIT " + BATCH_SIZE));

        for (MessageOutbox record : records) {
            try {
                republishOutbox(record);
            } catch (Exception e) {
                markRepublishFailure(record, e);
                log.error("Outbox republish failed, id={}", record.getId(), e);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void republishOutbox(MessageOutbox record) {
        MessageOutbox failedCopy = record.withSendFailure(MAX_RETRY_COUNT, RETRY_BASE_DELAY_SECONDS);
        if ("DEAD".equals(failedCopy.getSendStatus())) {
            messageOutboxMapper.updateById(failedCopy);
            log.warn("Outbox exceeded max retries and marked DEAD, id={}", record.getId());
            return;
        }

        if (record.getExchangeName() == null || record.getExchangeName().isBlank()) {
            rabbitTemplate.convertAndSend(record.getRoutingKey(), record.getMessageBody());
        } else {
            rabbitTemplate.convertAndSend(record.getExchangeName(), record.getRoutingKey(), record.getMessageBody());
        }
        MessageOutbox successCopy = record.withSendSuccess();
        messageOutboxMapper.updateById(successCopy);
        log.info("Outbox republished, id={}, bizType={}, bizNo={}", record.getId(), record.getBizType(), record.getBizNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public void markRepublishFailure(MessageOutbox record, Exception ex) {
        MessageOutbox failedCopy = record.withSendFailure(MAX_RETRY_COUNT, RETRY_BASE_DELAY_SECONDS);
        messageOutboxMapper.updateById(failedCopy);
        log.warn("Outbox republish scheduled retry, id={}, nextRetryTime={}, retryCount={}",
                failedCopy.getId(), failedCopy.getNextRetryTime(), failedCopy.getRetryCount(), ex);
    }
}
