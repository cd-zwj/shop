package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.entity.MessageIdempotent;
import com.payment.mapper.MessageIdempotentMapper;
import com.payment.service.MessageIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        return record != null;
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
        } catch (Exception e) {
            // 如果插入失败（可能是重复），记录日志但不抛出异常
            log.warn("记录消息处理成功失败（可能已存在），messageId: {}, queueName: {}", messageId, queueName);
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
        } catch (Exception e) {
            log.error("记录消息处理失败失败，messageId: {}, queueName: {}", messageId, queueName, e);
        }
    }
}
