package com.payment.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.payment.config.RabbitMQConfig;
import com.payment.service.MessageIdempotentService;
import com.payment.service.sms.SmsSender;
import com.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 短信发送消费者
 * <p>
 * 从 {@link RabbitMQConfig#SMS_SEND_QUEUE} 队列中消费短信发送请求消息，
 * 调用 {@link SmsSender} 执行短信发送。
 * 通过 {@link MessageIdempotentService} 保障消息幂等性，避免重复发送短信。
 * </p>
 *
 * @author payment-system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsSendConsumer {

    /** 消息幂等服务，用于防止重复消费 */
    private final MessageIdempotentService messageIdempotentService;

    /** 短信发送器，执行实际的短信发送操作 */
    private final SmsSender smsSender;

    /**
     * 处理短信发送消息
     * <p>
     * 从消息体中解析短信场景、手机号和验证码等字段，
     * 进行幂等校验后调用短信发送器发送短信。
     * </p>
     *
     * @param body 消息体 JSON 字符串，包含 scene、phone、code 等字段
     * @throws IllegalArgumentException 当消息体缺少必填字段时抛出
     */
    @RabbitListener(queues = RabbitMQConfig.SMS_SEND_QUEUE)
    public void handleSmsSend(String body) {
        Map<String, Object> payload = JsonUtils.fromJson(body, new TypeReference<Map<String, Object>>() {
        });
        String scene = requireNonBlank(payload, "scene", body);
        String phone = requireNonBlank(payload, "phone", body);
        String code = requireNonBlank(payload, "code", body);
        String messageId = RabbitMQConfig.SMS_SEND_QUEUE + ":" + scene + ":" + phone + ":" + code;

        String claimToken = MessageClaimGuard.acquire(messageIdempotentService,
                messageId, RabbitMQConfig.SMS_SEND_QUEUE, body,
                SmsSendConsumer.class.getSimpleName());
        if (claimToken == null) {
            log.info("短信发送消息已处理,跳过 messageId={}", messageId);
            return;
        }

        try {
            smsSender.send(phone, code);
            messageIdempotentService.recordSuccess(
                    messageId,
                    RabbitMQConfig.SMS_SEND_QUEUE,
                    body,
                    SmsSendConsumer.class.getSimpleName(), claimToken);
        } catch (Exception e) {
            messageIdempotentService.recordFailure(
                    messageId,
                    RabbitMQConfig.SMS_SEND_QUEUE,
                    body,
                    SmsSendConsumer.class.getSimpleName(), claimToken,
                    e.getMessage());
            throw e;
        }
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
            throw new IllegalArgumentException("短信发送消息缺少 " + fieldName + ", body=" + body);
        }
        String value = rawValue.toString();
        if (value.isBlank()) {
            throw new IllegalArgumentException("短信发送消息 " + fieldName + " 为空, body=" + body);
        }
        return value;
    }
}
