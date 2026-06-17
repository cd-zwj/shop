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
 * 用户行为事件消费者。
 *
 * 当前版本先打通行为事件的异步消费与幂等记录，后续画像/推荐可从这里接入。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserBehaviorConsumer {

    private final MessageIdempotentService messageIdempotentService;

    @RabbitListener(queues = RabbitMQConfig.USER_BEHAVIOR_QUEUE)
    public void handleBehavior(String body) {
        Map<String, Object> payload = JsonUtils.fromJson(body, new TypeReference<Map<String, Object>>() {
        });
        String behaviorLogId = requireNonBlank(payload, "behaviorLogId", body);
        String messageId = RabbitMQConfig.USER_BEHAVIOR_QUEUE + ":" + behaviorLogId;

        if (messageIdempotentService.isProcessed(messageId, RabbitMQConfig.USER_BEHAVIOR_QUEUE)) {
            log.info("用户行为消息已处理,跳过 messageId={}", messageId);
            return;
        }

        try {
            processBehaviorEvent(payload);
            messageIdempotentService.recordSuccess(
                    messageId,
                    RabbitMQConfig.USER_BEHAVIOR_QUEUE,
                    body,
                    UserBehaviorConsumer.class.getSimpleName());
        } catch (Exception e) {
            messageIdempotentService.recordFailure(
                    messageId,
                    RabbitMQConfig.USER_BEHAVIOR_QUEUE,
                    body,
                    UserBehaviorConsumer.class.getSimpleName(),
                    e.getMessage());
            throw e;
        }
    }

    protected void processBehaviorEvent(Map<String, Object> payload) {
        log.info("用户行为事件已消费, behaviorLogId={}, platformUserId={}, behaviorType={}",
                payload.get("behaviorLogId"), payload.get("platformUserId"), payload.get("behaviorType"));
    }

    private String requireNonBlank(Map<String, Object> payload, String fieldName, String body) {
        Object rawValue = payload == null ? null : payload.get(fieldName);
        if (rawValue == null) {
            throw new IllegalArgumentException("用户行为消息缺少 " + fieldName + ", body=" + body);
        }
        String value = rawValue.toString();
        if (value.isBlank()) {
            throw new IllegalArgumentException("用户行为消息 " + fieldName + " 为空, body=" + body);
        }
        return value;
    }
}
