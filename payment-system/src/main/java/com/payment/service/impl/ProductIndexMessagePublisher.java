package com.payment.service.impl;

import com.payment.config.RabbitMQConfig;
import com.payment.dto.ProductIndexMessage;
import com.payment.entity.MessageOutbox;
import com.payment.entity.Product;
import com.payment.service.OutboxPublisher;
import com.payment.service.outbox.OutboxMessageCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 商品索引消息发布器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductIndexMessagePublisher {

    private static final String BIZ_TYPE_PRODUCT_INDEX = "PRODUCT_INDEX";

    private final OutboxPublisher outboxPublisher;

    public void publishUpsert(Product product) {
        publish(ProductIndexMessage.upsert(product));
    }

    public void publishDelete(Product product) {
        publish(ProductIndexMessage.delete(product));
    }

    private void publish(ProductIndexMessage message) {
        MessageOutbox outbox = outboxPublisher.publish(OutboxMessageCommand.builder()
                .bizType(BIZ_TYPE_PRODUCT_INDEX)
                .bizNo(String.valueOf(message.getId()))
                .routingKey(RabbitMQConfig.PRODUCT_INDEX_QUEUE)
                .messageBody(message)
                .build());

        log.info("商品索引消息已写入Outbox，action={}, tenantId={}, productId={}, outboxId={}",
                message.getAction(), message.getTenantId(), message.getId(), outbox.getId());
    }
}
