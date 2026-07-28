package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.entity.MessageIdempotent;
import com.payment.mapper.MessageIdempotentMapper;
import com.payment.service.MessageClaim;
import com.payment.service.MessageClaimResult;
import com.payment.service.MessageIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 消息幂等性服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageIdempotentServiceImpl implements MessageIdempotentService {

    private static final int PROCESSING_STALE_MINUTES = 5;
    private static final String CLAIM_PREFIX = "CLAIM:";

    private final MessageIdempotentMapper messageIdempotentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageClaim tryClaim(String messageId, String queueName, String messageBody, String consumerName) {
        validateIdentity(messageId, queueName, consumerName);
        LocalDateTime now = LocalDateTime.now();
        String token = UUID.randomUUID().toString();
        String claimMarker = CLAIM_PREFIX + token;
        try {
            if (messageIdempotentMapper.retryFailed(
                    messageId, queueName, messageBody, consumerName, claimMarker, now) == 1) {
                return MessageClaim.acquired(token);
            }
            if (messageIdempotentMapper.reclaimStale(
                    messageId, queueName, messageBody, consumerName, claimMarker, now,
                    now.minusMinutes(PROCESSING_STALE_MINUTES)) == 1) {
                return MessageClaim.acquired(token);
            }
            if (messageIdempotentMapper.insertProcessing(
                    messageId, queueName, messageBody, consumerName, claimMarker, now) == 1) {
                return MessageClaim.acquired(token);
            }
        } catch (DeadlockLoserDataAccessException exception) {
            log.info("消息抢占并发冲突，将由消费端重排，messageId: {}, queueName: {}", messageId, queueName);
            return MessageClaim.inProgress();
        }
        MessageIdempotent record = messageIdempotentMapper.selectById(messageId);
        if (record == null || !queueName.equals(record.getQueueName())) {
            throw new IllegalStateException("消息 ID 已被其他队列占用: " + messageId);
        }
        return Integer.valueOf(1).equals(record.getStatus())
                ? MessageClaim.completed()
                : MessageClaim.inProgress();
    }

    @Override
    public boolean isProcessed(String messageId, String queueName) {
        MessageIdempotent record = messageIdempotentMapper.selectOne(
                new LambdaQueryWrapper<MessageIdempotent>()
                        .eq(MessageIdempotent::getMessageId, messageId)
                        .eq(MessageIdempotent::getQueueName, queueName)
        );
        return record != null && record.getStatus() != null && record.getStatus() == 1;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordSuccess(String messageId, String queueName, String messageBody,
                              String consumerName, String claimToken) {
        validateIdentityAndToken(messageId, queueName, consumerName, claimToken);
        if (messageIdempotentMapper.finishProcessing(
                messageId, queueName, messageBody, consumerName, 1,
                CLAIM_PREFIX + claimToken, null, LocalDateTime.now()) != 1) {
            throw new IllegalStateException("消息未取得处理权，无法记录成功: " + messageId);
        }
        log.info("记录消息处理成功，messageId: {}, queueName: {}", messageId, queueName);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordFailure(String messageId, String queueName, String messageBody,
                              String consumerName, String claimToken, String errorMessage) {
        validateIdentityAndToken(messageId, queueName, consumerName, claimToken);
        if (messageIdempotentMapper.finishProcessing(
                messageId, queueName, messageBody, consumerName, 2,
                CLAIM_PREFIX + claimToken, errorMessage, LocalDateTime.now()) != 1) {
            throw new IllegalStateException("消息未取得处理权，无法记录失败: " + messageId);
        }
        log.error("记录消息处理失败，messageId: {}, queueName: {}, error: {}", messageId, queueName, errorMessage);
    }

    private void validateIdentity(String messageId, String queueName, String consumerName) {
        requireText(messageId, "messageId", 100);
        requireText(queueName, "queueName", 100);
        requireText(consumerName, "consumerName", 100);
    }

    private void validateIdentityAndToken(String messageId, String queueName,
                                          String consumerName, String claimToken) {
        validateIdentity(messageId, queueName, consumerName);
        requireText(claimToken, "claimToken", 64);
    }

    private void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException(field + " must contain 1-" + maxLength + " characters");
        }
    }
}
