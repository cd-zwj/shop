package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.entity.MessageIdempotent;
import com.payment.mapper.MessageIdempotentMapper;
import com.payment.service.MessageClaimResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageIdempotentServiceImplTest {

    @Test
    void tryClaimShouldAcquireNewMessage() {
        MessageIdempotentMapper mapper = mock(MessageIdempotentMapper.class);
        MessageIdempotentServiceImpl service = new MessageIdempotentServiceImpl(mapper);
        when(mapper.insertProcessing(any(), any(), any(), any(), any(), any())).thenReturn(1);

        assertEquals(MessageClaimResult.ACQUIRED,
                service.tryClaim("MSG-1", "queue", "{}", "consumer").result());
        verify(mapper).insertProcessing(any(), any(), any(), any(), any(), any());
    }

    @Test
    void tryClaimShouldRetryOnlyFailedMessage() {
        MessageIdempotentMapper mapper = mock(MessageIdempotentMapper.class);
        MessageIdempotentServiceImpl service = new MessageIdempotentServiceImpl(mapper);
        when(mapper.retryFailed(any(), any(), any(), any(), any(), any())).thenReturn(1);

        assertEquals(MessageClaimResult.ACQUIRED,
                service.tryClaim("MSG-1", "queue", "{}", "consumer").result());
    }

    @Test
    void tryClaimShouldSkipSuccessfulOrProcessingMessage() {
        MessageIdempotentMapper mapper = mock(MessageIdempotentMapper.class);
        MessageIdempotentServiceImpl service = new MessageIdempotentServiceImpl(mapper);

        MessageIdempotent processing = new MessageIdempotent();
        processing.setMessageId("MSG-1");
        processing.setQueueName("queue");
        processing.setStatus(0);
        when(mapper.selectById("MSG-1")).thenReturn(processing);

        assertEquals(MessageClaimResult.IN_PROGRESS,
                service.tryClaim("MSG-1", "queue", "{}", "consumer").result());
    }

    @Test
    void recordSuccessShouldRequireOwnedProcessingClaim() {
        MessageIdempotentMapper mapper = mock(MessageIdempotentMapper.class);
        MessageIdempotentServiceImpl service = new MessageIdempotentServiceImpl(mapper);

        assertThrows(IllegalStateException.class,
                () -> service.recordSuccess("MSG-1", "queue", "{}", "consumer", "claim-token"));
    }

    @Test
    void isProcessedShouldQueryByMessageIdAndQueueName() {
        MessageIdempotentMapper messageIdempotentMapper = mock(MessageIdempotentMapper.class);
        MessageIdempotentServiceImpl service = new MessageIdempotentServiceImpl(messageIdempotentMapper);

        MessageIdempotent record = new MessageIdempotent();
        record.setMessageId("MSG-1");
        record.setQueueName("payment.v1.order.paid");
        record.setStatus(1);
        when(messageIdempotentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

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

    @Test
    void isProcessedShouldReturnFalseWhenRecordIsFailure() {
        MessageIdempotentMapper messageIdempotentMapper = mock(MessageIdempotentMapper.class);
        MessageIdempotentServiceImpl service = new MessageIdempotentServiceImpl(messageIdempotentMapper);

        MessageIdempotent record = new MessageIdempotent();
        record.setMessageId("MSG-1");
        record.setQueueName("payment.v1.order.paid");
        record.setStatus(2);
        when(messageIdempotentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        assertFalse(service.isProcessed("MSG-1", "payment.v1.order.paid"));
    }
}
