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
 * 优惠券领域事件消费者
 * <p>
 * 从 {@link RabbitMQConfig#COUPON_EVENT_QUEUE} 队列中消费优惠券状态变更事件。
 * 优惠券状态变更已在事务内落库并写入 Outbox，这里负责消费确认与后续扩展入口。
 * 通过 {@link MessageIdempotentService} 保障消息幂等性。
 * </p>
 *
 * @author payment-system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventConsumer {

    /**
     * 消息幂等服务，用于防止重复消费
     */
    private final MessageIdempotentService messageIdempotentService;

    /**
     * 处理优惠券事件消息
     * <p>
     * 从消息体中解析事件类型、用户优惠券 ID、业务编号等关键字段，
     * 进行幂等校验后执行事件处理逻辑。
     * </p>
     *
     * @param body 消息体 JSON 字符串，包含 eventType、userCouponId、bizNo 等字段
     * @throws IllegalArgumentException 当消息体缺少必填字段时抛出
     */
    @RabbitListener(queues = RabbitMQConfig.COUPON_EVENT_QUEUE)
    public void handleCouponEvent(String body) {
        Map<String, Object> payload = JsonUtils.fromJson(body, new TypeReference<Map<String, Object>>() {
        });
        String eventType = requireNonBlank(payload, "eventType", body);
        String userCouponId = requireNonBlank(payload, "userCouponId", body);
        String bizNo = requireNonBlank(payload, "bizNo", body);
        String messageId = RabbitMQConfig.COUPON_EVENT_QUEUE + ":" + eventType + ":" + userCouponId + ":" + bizNo;

        String claimToken = MessageClaimGuard.acquire(messageIdempotentService,
                messageId, RabbitMQConfig.COUPON_EVENT_QUEUE, body,
                CouponEventConsumer.class.getSimpleName());
        if (claimToken == null) {
            log.info("优惠券事件消息已处理,跳过 messageId={}", messageId);
            return;
        }

        try {
            processCouponEvent(payload);
            messageIdempotentService.recordSuccess(
                    messageId,
                    RabbitMQConfig.COUPON_EVENT_QUEUE,
                    body,
                    CouponEventConsumer.class.getSimpleName(), claimToken);
        } catch (Exception e) {
            messageIdempotentService.recordFailure(
                    messageId,
                    RabbitMQConfig.COUPON_EVENT_QUEUE,
                    body,
                    CouponEventConsumer.class.getSimpleName(), claimToken,
                    e.getMessage());
            throw e;
        }
    }

    /**
     * 处理优惠券事件的具体业务逻辑
     * <p>
     * 当前版本仅记录日志，后续可扩展为触发通知、更新统计等操作。
     * </p>
     *
     * @param payload 解析后的消息体 Map
     */
    protected void processCouponEvent(Map<String, Object> payload) {
        log.info("优惠券事件已消费, eventType={}, userCouponId={}, couponStatus={}, orderNo={}",
                payload.get("eventType"), payload.get("userCouponId"), payload.get("couponStatus"), payload.get("orderNo"));
    }

    /**
     * 校验消息载荷中的字段非空
     *
     * @param payload   消息载荷 Map
     * @param fieldName 字段名
     * @param body      原始消息体（用于异常信息输出）
     * @return 字段值的字符串表示
     * @throws IllegalArgumentException 当字段不存在或值为空白字符串时抛出
     */
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
