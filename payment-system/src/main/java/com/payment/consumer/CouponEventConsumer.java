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
 * 优惠券领域事件消费者。
 *
 * 优惠券状态变更已在事务内落库并写入 Outbox，这里负责消费确认与后续扩展入口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventConsumer {

    private final MessageIdempotentService messageIdempotentService;

    @RabbitListener(queues = RabbitMQConfig.COUPON_EVENT_QUEUE)
    public void handleCouponEvent(String body) {
        Map<String, Object> payload = JsonUtils.fromJson(body, new TypeReference<Map<String, Object>>() {
        });
        String eventType = requireNonBlank(payload, "eventType", body);
        String userCouponId = requireNonBlank(payload, "userCouponId", body);
        String bizNo = requireNonBlank(payload, "bizNo", body);
        String messageId = RabbitMQConfig.COUPON_EVENT_QUEUE + ":" + eventType + ":" + userCouponId + ":" + bizNo;

        if (messageIdempotentService.isProcessed(messageId, RabbitMQConfig.COUPON_EVENT_QUEUE)) {
            log.info("优惠券事件消息已处理,跳过 messageId={}", messageId);
            return;
        }

        try {
            processCouponEvent(payload);
            messageIdempotentService.recordSuccess(
                    messageId,
                    RabbitMQConfig.COUPON_EVENT_QUEUE,
                    body,
                    CouponEventConsumer.class.getSimpleName());
        } catch (Exception e) {
            messageIdempotentService.recordFailure(
                    messageId,
                    RabbitMQConfig.COUPON_EVENT_QUEUE,
                    body,
                    CouponEventConsumer.class.getSimpleName(),
                    e.getMessage());
            throw e;
        }
    }

    protected void processCouponEvent(Map<String, Object> payload) {
        log.info("优惠券事件已消费, eventType={}, userCouponId={}, couponStatus={}, orderNo={}",
                payload.get("eventType"), payload.get("userCouponId"), payload.get("couponStatus"), payload.get("orderNo"));
    }

    private String requireNonBlank(Map<String, Object> payload, String fieldName, String body) {
        Object rawValue = payload == null ? null : payload.get(fieldName);
        if (rawValue == null) {
            throw new IllegalArgumentException("优惠券事件消息缺少 " + fieldName + ", body=" + body);
        }
        String value = rawValue.toString();
        if (value.isBlank()) {
            throw new IllegalArgumentException("优惠券事件消息 " + fieldName + " 为空, body=" + body);
        }
        return value;
    }
}
