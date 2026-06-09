package com.payment.service.impl;

import com.payment.entity.MessageOutbox;
import com.payment.mapper.MessageOutboxMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageOutboxRetrySchedulerTest {

    @Test
    void shouldRepublishPendingOutboxAndMarkSent() {
        MessageOutboxMapper messageOutboxMapper = mock(MessageOutboxMapper.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        MessageOutboxRetryScheduler scheduler = new MessageOutboxRetryScheduler(messageOutboxMapper, rabbitTemplate);

        MessageOutbox record = new MessageOutbox();
        record.setId(1L);
        record.setMessageId("MSG1");
        record.setBizType("SALES_ORDER");
        record.setBizNo("SO1");
        record.setExchangeName("");
        record.setRoutingKey("payment.v1.order.paid");
        record.setMessageBody("{\"billNo\":\"PB1\"}");
        record.setSendStatus("PENDING");
        record.setRetryCount(0);
        record.setNextRetryTime(LocalDateTime.now().minusMinutes(1));

        when(messageOutboxMapper.selectList(any())).thenReturn(List.of(record));

        scheduler.retryPendingOutbox();

        verify(rabbitTemplate).convertAndSend(eq("payment.v1.order.paid"), eq("{\"billNo\":\"PB1\"}"));
        ArgumentCaptor<MessageOutbox> captor = ArgumentCaptor.forClass(MessageOutbox.class);
        verify(messageOutboxMapper).updateById(captor.capture());
        assertEquals("SENT", captor.getValue().getSendStatus());
    }

    @Test
    void shouldMarkDeadWhenMaxRetriesReached() {
        MessageOutboxMapper messageOutboxMapper = mock(MessageOutboxMapper.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        MessageOutboxRetryScheduler scheduler = new MessageOutboxRetryScheduler(messageOutboxMapper, rabbitTemplate);

        MessageOutbox record = new MessageOutbox();
        record.setId(2L);
        record.setMessageId("MSG2");
        record.setSendStatus("FAILED");
        record.setRetryCount(15);
        record.setNextRetryTime(LocalDateTime.now());
        record.setRoutingKey("payment.v1.order.paid");
        record.setMessageBody("{}");

        when(messageOutboxMapper.selectList(any())).thenReturn(List.of(record));

        scheduler.retryPendingOutbox();

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class));

        ArgumentCaptor<MessageOutbox> captor = ArgumentCaptor.forClass(MessageOutbox.class);
        verify(messageOutboxMapper).updateById(captor.capture());
        assertEquals("DEAD", captor.getValue().getSendStatus());
        assertNull(captor.getValue().getNextRetryTime());
    }
}
