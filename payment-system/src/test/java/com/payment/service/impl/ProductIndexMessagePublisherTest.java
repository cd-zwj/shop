package com.payment.service.impl;

import com.payment.config.RabbitMQConfig;
import com.payment.dto.ProductIndexMessage;
import com.payment.entity.Product;
import com.payment.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ProductIndexMessagePublisherTest {

    @Test
    void publishUpsertShouldSendProductIndexMessage() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ProductIndexMessagePublisher publisher = new ProductIndexMessagePublisher(rabbitTemplate);
        Product product = buildProduct();

        publisher.publishUpsert(product);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.PRODUCT_INDEX_QUEUE), bodyCaptor.capture());

        ProductIndexMessage message = JsonUtils.fromJson(bodyCaptor.getValue(), ProductIndexMessage.class);
        assertEquals(ProductIndexMessage.ACTION_UPSERT, message.getAction());
        assertEquals(product.getId(), message.getId());
        assertEquals(product.getTenantId(), message.getTenantId());
        assertEquals(product.getName(), message.getName());
    }

    @Test
    void publishDeleteShouldSendDeleteAction() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ProductIndexMessagePublisher publisher = new ProductIndexMessagePublisher(rabbitTemplate);
        Product product = buildProduct();

        publisher.publishDelete(product);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.PRODUCT_INDEX_QUEUE), bodyCaptor.capture());

        ProductIndexMessage message = JsonUtils.fromJson(bodyCaptor.getValue(), ProductIndexMessage.class);
        assertEquals(ProductIndexMessage.ACTION_DELETE, message.getAction());
        assertEquals(product.getId(), message.getId());
    }

    @Test
    void publishUpsertShouldSendAfterTransactionCommit() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ProductIndexMessagePublisher publisher = new ProductIndexMessagePublisher(rabbitTemplate);
        Product product = buildProduct();

        TransactionSynchronizationManager.initSynchronization();
        try {
            publisher.publishUpsert(product);

            verify(rabbitTemplate, never()).convertAndSend(eq(RabbitMQConfig.PRODUCT_INDEX_QUEUE), org.mockito.ArgumentMatchers.any(String.class));

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.PRODUCT_INDEX_QUEUE), bodyCaptor.capture());
            ProductIndexMessage message = JsonUtils.fromJson(bodyCaptor.getValue(), ProductIndexMessage.class);
            assertEquals(ProductIndexMessage.ACTION_UPSERT, message.getAction());
            assertEquals(product.getId(), message.getId());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishUpsertShouldNotSendWhenTransactionRollsBack() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ProductIndexMessagePublisher publisher = new ProductIndexMessagePublisher(rabbitTemplate);

        TransactionSynchronizationManager.initSynchronization();
        try {
            publisher.publishUpsert(buildProduct());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(rabbitTemplate, never()).convertAndSend(eq(RabbitMQConfig.PRODUCT_INDEX_QUEUE), org.mockito.ArgumentMatchers.any(String.class));
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
