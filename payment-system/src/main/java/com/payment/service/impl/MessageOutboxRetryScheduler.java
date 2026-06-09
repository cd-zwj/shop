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

        rabbitTemplate.convertAndSend(record.getRoutingKey(), record.getMessageBody());
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
