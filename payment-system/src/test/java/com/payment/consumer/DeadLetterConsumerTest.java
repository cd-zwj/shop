package com.payment.consumer;

import com.payment.entity.DeadLetterTask;
import com.payment.entity.RechargeOrder;
import com.payment.mapper.DeadLetterTaskMapper;
import com.payment.mapper.RechargeOrderMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeadLetterConsumerTest {

    @Test
    void shouldPersistDeadLetterForNonRechargeMessage() throws Exception {
        RechargeOrderMapper rechargeOrderMapper = mock(RechargeOrderMapper.class);
        DeadLetterTaskMapper deadLetterTaskMapper = mock(DeadLetterTaskMapper.class);
        DeadLetterConsumer consumer = new DeadLetterConsumer(rechargeOrderMapper, deadLetterTaskMapper);
        Channel channel = mock(Channel.class);

        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(11L);
        properties.setReceivedExchange("payment.exchange");
        properties.setReceivedRoutingKey("payment.v1.order.paid");
        properties.setHeader("x-first-death-reason", "rejected");
        properties.getHeaders().put("x-death", List.of(Map.of("queue", "payment.v1.order.paid", "reason", "rejected")));
        Message message = new Message("body".getBytes(StandardCharsets.UTF_8), properties);

        consumer.handleDeadLetter(message, channel);

        ArgumentCaptor<DeadLetterTask> captor = ArgumentCaptor.forClass(DeadLetterTask.class);
        verify(deadLetterTaskMapper).insert(captor.capture());
        assertEquals("payment.v1.order.paid", captor.getValue().getQueueName());
        assertEquals("PENDING", captor.getValue().getHandleStatus());
        verify(channel).basicAck(11L, false);
        verify(rechargeOrderMapper, never()).selectOne(any());
    }

    @Test
    void shouldHandleRechargeTimeoutWithoutPersistingDeadLetter() throws Exception {
        RechargeOrderMapper rechargeOrderMapper = mock(RechargeOrderMapper.class);
        DeadLetterTaskMapper deadLetterTaskMapper = mock(DeadLetterTaskMapper.class);
        DeadLetterConsumer consumer = new DeadLetterConsumer(rechargeOrderMapper, deadLetterTaskMapper);
        Channel channel = mock(Channel.class);

        RechargeOrder order = new RechargeOrder();
        order.setPayStatus("0");

        when(rechargeOrderMapper.selectOne(any())).thenReturn(order);

        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(22L);
        properties.getHeaders().put("x-death", List.of(Map.of("queue", "payment.recharge.delay")));
        Message message = new Message("RO123".getBytes(StandardCharsets.UTF_8), properties);

        consumer.handleDeadLetter(message, channel);

        verify(rechargeOrderMapper).selectOne(any());
        verify(deadLetterTaskMapper, never()).insert(any());
        verify(channel).basicAck(22L, false);
    }
}
