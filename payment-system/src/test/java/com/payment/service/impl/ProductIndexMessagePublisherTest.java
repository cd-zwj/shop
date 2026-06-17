package com.payment.service.impl;

import com.payment.config.RabbitMQConfig;
import com.payment.dto.ProductIndexMessage;
import com.payment.entity.MessageOutbox;
import com.payment.entity.Product;
import com.payment.enums.OutboxSendStatusEnum;
import com.payment.mapper.MessageOutboxMapper;
import com.payment.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductIndexMessagePublisherTest {

    @Test
    void publishUpsertShouldInsertPendingOutboxRecord() {
        MessageOutboxMapper messageOutboxMapper = mock(MessageOutboxMapper.class);
        ProductIndexMessagePublisher publisher = new ProductIndexMessagePublisher(new OutboxPublisherImpl(messageOutboxMapper));
        Product product = buildProduct();

        publisher.publishUpsert(product);

        ArgumentCaptor<MessageOutbox> outboxCaptor = ArgumentCaptor.forClass(MessageOutbox.class);
        verify(messageOutboxMapper).insert(outboxCaptor.capture());

        MessageOutbox outbox = outboxCaptor.getValue();
        assertNotNull(outbox.getMessageId());
        assertEquals("PRODUCT_INDEX", outbox.getBizType());
        assertEquals(String.valueOf(product.getId()), outbox.getBizNo());
        assertEquals("", outbox.getExchangeName());
        assertEquals(RabbitMQConfig.PRODUCT_INDEX_QUEUE, outbox.getRoutingKey());
        assertEquals(OutboxSendStatusEnum.PENDING.name(), outbox.getSendStatus());
        assertEquals(0, outbox.getRetryCount());
        assertNotNull(outbox.getNextRetryTime());

        ProductIndexMessage message = JsonUtils.fromJson(outbox.getMessageBody(), ProductIndexMessage.class);
        assertEquals(ProductIndexMessage.ACTION_UPSERT, message.getAction());
        assertEquals(product.getId(), message.getId());
        assertEquals(product.getTenantId(), message.getTenantId());
        assertEquals(product.getName(), message.getName());
    }

    @Test
    void publishDeleteShouldInsertDeleteActionOutboxRecord() {
        MessageOutboxMapper messageOutboxMapper = mock(MessageOutboxMapper.class);
        ProductIndexMessagePublisher publisher = new ProductIndexMessagePublisher(new OutboxPublisherImpl(messageOutboxMapper));
        Product product = buildProduct();

        publisher.publishDelete(product);

        ArgumentCaptor<MessageOutbox> outboxCaptor = ArgumentCaptor.forClass(MessageOutbox.class);
        verify(messageOutboxMapper).insert(outboxCaptor.capture());

        ProductIndexMessage message = JsonUtils.fromJson(outboxCaptor.getValue().getMessageBody(), ProductIndexMessage.class);
        assertEquals(ProductIndexMessage.ACTION_DELETE, message.getAction());
        assertEquals(product.getId(), message.getId());
    }

    @Test
    void publishUpsertShouldInsertOutboxInsideTransaction() {
        MessageOutboxMapper messageOutboxMapper = mock(MessageOutboxMapper.class);
        ProductIndexMessagePublisher publisher = new ProductIndexMessagePublisher(new OutboxPublisherImpl(messageOutboxMapper));
        Product product = buildProduct();

        TransactionSynchronizationManager.initSynchronization();
        try {
            publisher.publishUpsert(product);

            ArgumentCaptor<MessageOutbox> outboxCaptor = ArgumentCaptor.forClass(MessageOutbox.class);
            verify(messageOutboxMapper).insert(outboxCaptor.capture());
            assertEquals(OutboxSendStatusEnum.PENDING.name(), outboxCaptor.getValue().getSendStatus());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private Product buildProduct() {
        Product product = new Product();
        product.setId(101L);
        product.setTenantId(2001L);
        product.setProductCode("PRD-101");
        product.setName("可乐");
        product.setPrice(new BigDecimal("3.50"));
        product.setStatus(1);
        product.setDeleted(0);
        return product;
    }
}
