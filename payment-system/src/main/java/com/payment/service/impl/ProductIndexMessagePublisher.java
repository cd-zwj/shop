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
 * <p>
 * 负责将商品的新增/更新/删除事件以消息的形式发布到 Outbox 表，
 * 再由 Outbox 异步投递至 RabbitMQ，最终由消费者同步到 Elasticsearch 索引。
 * 采用 Outbox 模式保证消息投递与数据库事务的一致性，避免消息丢失。
 * </p>
 *
 * @see OutboxPublisher
 * @see com.payment.consumer.ProductIndexMessageConsumer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductIndexMessagePublisher {

    private static final String BIZ_TYPE_PRODUCT_INDEX = "PRODUCT_INDEX";

    private final OutboxPublisher outboxPublisher;

    /**
     * 发布商品新增或更新的索引消息。
     * <p>
     * 当商品被创建或修改后调用，生成 upsert 类型的索引消息并写入 Outbox，
     * 由下游消费者将商品数据同步写入 Elasticsearch。
     * </p>
     *
     * @param product 发生变更的商品实体
     */
    public void publishUpsert(Product product) {
        publish(ProductIndexMessage.upsert(product));
    }

    /**
     * 发布商品删除的索引消息。
     * <p>
     * 当商品被删除时调用，生成 delete 类型的索引消息并写入 Outbox，
     * 由下游消费者从 Elasticsearch 中移除对应的商品文档。
     * </p>
     *
     * @param product 被删除的商品实体
     */
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
