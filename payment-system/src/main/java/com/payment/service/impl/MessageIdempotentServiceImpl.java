package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.payment.entity.MessageIdempotent;
import com.payment.mapper.MessageIdempotentMapper;
import com.payment.service.MessageIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 消息幂等性服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageIdempotentServiceImpl implements MessageIdempotentService {

    private final MessageIdempotentMapper messageIdempotentMapper;
    
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
    public void recordSuccess(String messageId, String queueName, String messageBody, String consumerName) {
        MessageIdempotent record = new MessageIdempotent();
        record.setMessageId(messageId);
        record.setQueueName(queueName);
        record.setMessageBody(messageBody);
        record.setConsumerName(consumerName);
        record.setStatus(1); // 处理成功
        record.setRetryCount(0);
        record.setCreatedTime(LocalDateTime.now());
        record.setUpdatedTime(LocalDateTime.now());
        
        try {
            messageIdempotentMapper.insert(record);
            log.info("记录消息处理成功，messageId: {}, queueName: {}", messageId, queueName);
        } catch (DataIntegrityViolationException e) {
            log.warn("消息幂等记录已存在，执行更新，messageId: {}, queueName: {}", messageId, queueName);
            messageIdempotentMapper.update(null, new LambdaUpdateWrapper<MessageIdempotent>()
                    .eq(MessageIdempotent::getMessageId, messageId)
                    .eq(MessageIdempotent::getQueueName, queueName)
                    .set(MessageIdempotent::getStatus, record.getStatus())
                    .set(MessageIdempotent::getConsumerName, consumerName)
                    .set(MessageIdempotent::getMessageBody, messageBody)
                    .set(MessageIdempotent::getUpdatedTime, LocalDateTime.now()));
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordFailure(String messageId, String queueName, String messageBody, String consumerName, String errorMessage) {
        MessageIdempotent record = new MessageIdempotent();
        record.setMessageId(messageId);
        record.setQueueName(queueName);
        record.setMessageBody(messageBody);
        record.setConsumerName(consumerName);
        record.setStatus(2); // 处理失败
        record.setRetryCount(0);
        record.setErrorMessage(errorMessage);
        record.setCreatedTime(LocalDateTime.now());
        record.setUpdatedTime(LocalDateTime.now());
        
        try {
            messageIdempotentMapper.insert(record);
            log.error("记录消息处理失败，messageId: {}, queueName: {}, error: {}", messageId, queueName, errorMessage);
        } catch (DataIntegrityViolationException e) {
            log.warn("消息幂等记录已存在，执行失败状态更新，messageId: {}, queueName: {}", messageId, queueName);
            messageIdempotentMapper.update(null, new LambdaUpdateWrapper<MessageIdempotent>()
                    .eq(MessageIdempotent::getMessageId, messageId)
                    .eq(MessageIdempotent::getQueueName, queueName)
                    .set(MessageIdempotent::getStatus, record.getStatus())
                    .set(MessageIdempotent::getErrorMessage, errorMessage)
                    .set(MessageIdempotent::getConsumerName, consumerName)
                    .set(MessageIdempotent::getMessageBody, messageBody)
                    .set(MessageIdempotent::getUpdatedTime, LocalDateTime.now()));
        }
    }
}
