package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.entity.MessageIdempotent;
import com.payment.mapper.MessageIdempotentMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageIdempotentServiceImplTest {

    @Test
    void isProcessedShouldQueryByMessageIdAndQueueName() {
        MessageIdempotentMapper messageIdempotentMapper = mock(MessageIdempotentMapper.class);
        MessageIdempotentServiceImpl service = new MessageIdempotentServiceImpl(messageIdempotentMapper);

        when(messageIdempotentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(new MessageIdempotent());

        assertTrue(service.isProcessed("MSG-1", "payment.v1.order.paid"));
        verify(messageIdempotentMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void isProcessedShouldReturnFalseWhenNoRecord() {
        MessageIdempotentMapper messageIdempotentMapper = mock(MessageIdempotentMapper.class);
        MessageIdempotentServiceImpl service = new MessageIdempotentServiceImpl(messageIdempotentMapper);

        when(messageIdempotentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertFalse(service.isProcessed("MSG-1", "payment.v1.order.paid"));
    }
}
