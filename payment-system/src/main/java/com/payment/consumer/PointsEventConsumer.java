package com.payment.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.payment.config.RabbitMQConfig;
import com.payment.service.MessageIdempotentService;
import com.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 积分事件消费者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointsEventConsumer {

    private final MessageIdempotentService messageIdempotentService;

    @RabbitListener(queues = RabbitMQConfig.POINTS_EVENT_QUEUE)
    public void handlePointsEvent(String body) {
        Map<String, Object> payload = JsonUtils.fromJson(body, new TypeReference<Map<String, Object>>() {
        });
        String eventType = requireNonBlank(payload, "eventType", body);
        String bizType = requireNonBlank(payload, "bizType", body);
        String bizNo = requireNonBlank(payload, "bizNo", body);
        String messageId = RabbitMQConfig.POINTS_EVENT_QUEUE + ":" + eventType + ":" + bizType + ":" + bizNo;

        if (messageIdempotentService.isProcessed(messageId, RabbitMQConfig.POINTS_EVENT_QUEUE)) {
            log.info("积分事件消息已处理,跳过 messageId={}", messageId);
            return;
        }

        try {
            processPointsEvent(payload);
            messageIdempotentService.recordSuccess(
                    messageId,
                    RabbitMQConfig.POINTS_EVENT_QUEUE,
                    body,
                    PointsEventConsumer.class.getSimpleName());
        } catch (Exception e) {
            messageIdempotentService.recordFailure(
                    messageId,
                    RabbitMQConfig.POINTS_EVENT_QUEUE,
                    body,
                    PointsEventConsumer.class.getSimpleName(),
                    e.getMessage());
            throw e;
        }
    }

    protected void processPointsEvent(Map<String, Object> payload) {
        // TODO: v1 only records durable consumption. Wire profile/AI/notification projections here when those consumers exist.
        log.info("积分事件已消费, eventType={}, tenantId={}, platformUserId={}, bizType={}, bizNo={}",
                payload.get("eventType"), payload.get("tenantId"), payload.get("platformUserId"),
                payload.get("bizType"), payload.get("bizNo"));
    }

    private String requireNonBlank(Map<String, Object> payload, String fieldName, String body) {
        Object rawValue = payload == null ? null : payload.get(fieldName);
        if (rawValue == null) {
            throw new IllegalArgumentException("积分事件消息缺少 " + fieldName + ", body=" + body);
        }
        String value = rawValue.toString();
        if (value.isBlank()) {
            throw new IllegalArgumentException("积分事件消息 " + fieldName + " 为空, body=" + body);
        }
        return value;
    }
}
