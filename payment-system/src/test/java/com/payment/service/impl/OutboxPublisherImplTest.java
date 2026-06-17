package com.payment.service.impl;

import com.payment.entity.MessageOutbox;
import com.payment.enums.OutboxSendStatusEnum;
import com.payment.mapper.MessageOutboxMapper;
import com.payment.service.outbox.OutboxMessageCommand;
import com.payment.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxPublisherImplTest {

    @Test
    void publishShouldInsertPendingOutboxWithSerializedBody() {
        MessageOutboxMapper mapper = mock(MessageOutboxMapper.class);
        OutboxPublisherImpl publisher = new OutboxPublisherImpl(mapper);

        MessageOutbox saved = publisher.publish(OutboxMessageCommand.builder()
                .messagePrefix("DLV")
                .bizType("ORDER_DELIVERY")
                .bizNo("ORDER123")
                .routingKey("payment.v1.order.delivery")
                .messageBody(Map.of("bizNo", "ORDER123", "bizType", "ORDER_DELIVERY"))
                .build());

        ArgumentCaptor<MessageOutbox> captor = ArgumentCaptor.forClass(MessageOutbox.class);
        verify(mapper).insert(captor.capture());
        MessageOutbox outbox = captor.getValue();
        assertEquals(outbox, saved);
        assertNotNull(outbox.getMessageId());
        assertEquals(true, outbox.getMessageId().startsWith("DLV"));
        assertEquals("ORDER_DELIVERY", outbox.getBizType());
        assertEquals("ORDER123", outbox.getBizNo());
        assertEquals("", outbox.getExchangeName());
        assertEquals("payment.v1.order.delivery", outbox.getRoutingKey());
        assertEquals(OutboxSendStatusEnum.PENDING.name(), outbox.getSendStatus());
        assertEquals(0, outbox.getRetryCount());
        assertNotNull(outbox.getNextRetryTime());

        Map<?, ?> body = JsonUtils.fromJson(outbox.getMessageBody(), Map.class);
        assertEquals("ORDER123", body.get("bizNo"));
        assertEquals("ORDER_DELIVERY", body.get("bizType"));
    }

    @Test
    void publishShouldDefaultMessagePrefixAndExchangeName() {
        MessageOutboxMapper mapper = mock(MessageOutboxMapper.class);
        OutboxPublisherImpl publisher = new OutboxPublisherImpl(mapper);

        publisher.publish(OutboxMessageCommand.builder()
                .bizType("PRODUCT_INDEX")
                .bizNo("101")
                .routingKey("product.index")
                .messageBody(Map.of("id", 101L))
                .build());

        ArgumentCaptor<MessageOutbox> captor = ArgumentCaptor.forClass(MessageOutbox.class);
        verify(mapper).insert(captor.capture());
        assertEquals(true, captor.getValue().getMessageId().startsWith("MSG"));
        assertEquals("", captor.getValue().getExchangeName());
    }
}
