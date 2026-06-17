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
 * 用户通知事件消费者。
 *
 * 当前通知已先落库，队列消费负责形成可观测、可重试的异步推送入口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserNotificationConsumer {

    private final MessageIdempotentService messageIdempotentService;

    @RabbitListener(queues = RabbitMQConfig.USER_NOTIFICATION_QUEUE)
    public void handleNotification(String body) {
        Map<String, Object> payload = JsonUtils.fromJson(body, new TypeReference<Map<String, Object>>() {
        });
        String notificationId = requireNonBlank(payload, "notificationId", body);
        String messageId = RabbitMQConfig.USER_NOTIFICATION_QUEUE + ":" + notificationId;

        if (messageIdempotentService.isProcessed(messageId, RabbitMQConfig.USER_NOTIFICATION_QUEUE)) {
            log.info("用户通知消息已处理,跳过 messageId={}", messageId);
            return;
        }

        try {
            processNotificationEvent(payload);
            messageIdempotentService.recordSuccess(
                    messageId,
                    RabbitMQConfig.USER_NOTIFICATION_QUEUE,
                    body,
                    UserNotificationConsumer.class.getSimpleName());
        } catch (Exception e) {
            messageIdempotentService.recordFailure(
                    messageId,
                    RabbitMQConfig.USER_NOTIFICATION_QUEUE,
                    body,
                    UserNotificationConsumer.class.getSimpleName(),
                    e.getMessage());
            throw e;
        }
    }

    protected void processNotificationEvent(Map<String, Object> payload) {
        log.info("用户通知事件已消费, notificationId={}, platformUserId={}, category={}",
                payload.get("notificationId"), payload.get("platformUserId"), payload.get("category"));
    }

    private String requireNonBlank(Map<String, Object> payload, String fieldName, String body) {
        Object rawValue = payload == null ? null : payload.get(fieldName);
        if (rawValue == null) {
            throw new IllegalArgumentException("用户通知消息缺少 " + fieldName + ", body=" + body);
        }
        String value = rawValue.toString();
        if (value.isBlank()) {
            throw new IllegalArgumentException("用户通知消息 " + fieldName + " 为空, body=" + body);
        }
        return value;
    }
}
