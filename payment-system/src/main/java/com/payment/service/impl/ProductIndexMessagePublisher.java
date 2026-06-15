package com.payment.service.impl;

import com.payment.config.RabbitMQConfig;
import com.payment.dto.ProductIndexMessage;
import com.payment.entity.MessageOutbox;
import com.payment.entity.Product;
import com.payment.enums.OutboxSendStatusEnum;
import com.payment.mapper.MessageOutboxMapper;
import com.payment.util.BizNoGenerator;
import com.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 商品索引消息发布器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductIndexMessagePublisher {

    private static final String BIZ_TYPE_PRODUCT_INDEX = "PRODUCT_INDEX";

    private final MessageOutboxMapper messageOutboxMapper;

    public void publishUpsert(Product product) {
        publish(ProductIndexMessage.upsert(product));
    }

    public void publishDelete(Product product) {
        publish(ProductIndexMessage.delete(product));
    }

    private void publish(ProductIndexMessage message) {
        MessageOutbox outbox = new MessageOutbox();
        outbox.setMessageId(BizNoGenerator.generate("MSG"));
        outbox.setBizType(BIZ_TYPE_PRODUCT_INDEX);
        outbox.setBizNo(String.valueOf(message.getId()));
        outbox.setExchangeName("");
        outbox.setRoutingKey(RabbitMQConfig.PRODUCT_INDEX_QUEUE);
        outbox.setMessageBody(JsonUtils.toJson(message));
        outbox.setSendStatus(OutboxSendStatusEnum.PENDING.name());
        outbox.setRetryCount(0);
        outbox.setNextRetryTime(LocalDateTime.now());
        messageOutboxMapper.insert(outbox);

        log.info("商品索引消息已写入Outbox，action={}, tenantId={}, productId={}, outboxId={}",
                message.getAction(), message.getTenantId(), message.getId(), outbox.getId());
    }
}
