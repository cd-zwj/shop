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
 * 用户行为事件消费者
 * <p>
 * 从 {@link RabbitMQConfig#USER_BEHAVIOR_QUEUE} 队列中消费用户行为事件。
 * 当前版本先打通行为事件的异步消费与幂等记录，后续画像/推荐可从这里接入。
 * 通过 {@link MessageIdempotentService} 保障消息幂等性。
 * </p>
 *
 * @author payment-system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserBehaviorConsumer {

    /** 消息幂等服务，用于防止重复消费 */
    private final MessageIdempotentService messageIdempotentService;

    /**
     * 处理用户行为事件消息
     * <p>
     * 从消息体中解析行为日志 ID，进行幂等校验后执行事件处理逻辑。
     * </p>
     *
     * @param body 消息体 JSON 字符串，必须包含 behaviorLogId 字段
     * @throws IllegalArgumentException 当消息体缺少必填字段时抛出
     */
    @RabbitListener(queues = RabbitMQConfig.USER_BEHAVIOR_QUEUE)
    public void handleBehavior(String body) {
        Map<String, Object> payload = JsonUtils.fromJson(body, new TypeReference<Map<String, Object>>() {
        });
        String behaviorLogId = requireNonBlank(payload, "behaviorLogId", body);
        String messageId = RabbitMQConfig.USER_BEHAVIOR_QUEUE + ":" + behaviorLogId;

        String claimToken = MessageClaimGuard.acquire(messageIdempotentService,
                messageId, RabbitMQConfig.USER_BEHAVIOR_QUEUE, body,
                UserBehaviorConsumer.class.getSimpleName());
        if (claimToken == null) {
            log.info("用户行为消息已处理,跳过 messageId={}", messageId);
            return;
        }

        try {
            processBehaviorEvent(payload);
            messageIdempotentService.recordSuccess(
                    messageId,
                    RabbitMQConfig.USER_BEHAVIOR_QUEUE,
                    body,
                    UserBehaviorConsumer.class.getSimpleName(), claimToken);
        } catch (Exception e) {
            messageIdempotentService.recordFailure(
                    messageId,
                    RabbitMQConfig.USER_BEHAVIOR_QUEUE,
                    body,
                    UserBehaviorConsumer.class.getSimpleName(), claimToken,
                    e.getMessage());
            throw e;
        }
    }

    /**
     * 处理用户行为事件的具体业务逻辑
     * <p>
     * 当前版本仅记录日志，后续可扩展为用户画像构建、推荐系统触发等操作。
     * </p>
     *
     * @param payload 解析后的消息体 Map
     */
    protected void processBehaviorEvent(Map<String, Object> payload) {
        log.info("用户行为事件已消费, behaviorLogId={}, platformUserId={}, behaviorType={}",
                payload.get("behaviorLogId"), payload.get("platformUserId"), payload.get("behaviorType"));
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
            throw new IllegalArgumentException("用户行为消息缺少 " + fieldName + ", body=" + body);
        }
        String value = rawValue.toString();
        if (value.isBlank()) {
            throw new IllegalArgumentException("用户行为消息 " + fieldName + " 为空, body=" + body);
        }
        return value;
    }
}
