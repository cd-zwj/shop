package com.payment.service.impl;

import com.payment.config.RabbitMQConfig;
import com.payment.dto.ProductIndexMessage;
import com.payment.entity.Product;
import com.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 商品索引消息发布器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductIndexMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishUpsert(Product product) {
        publish(ProductIndexMessage.upsert(product));
    }

    public void publishDelete(Product product) {
        publish(ProductIndexMessage.delete(product));
    }

    private void publish(ProductIndexMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendNow(message);
                }
            });
            return;
        }

        sendNow(message);
    }

    private void sendNow(ProductIndexMessage message) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.PRODUCT_INDEX_QUEUE, JsonUtils.toJson(message));
        log.info("发布商品索引消息成功，action={}, tenantId={}, productId={}",
                message.getAction(), message.getTenantId(), message.getId());
    }
}
