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
 * 短信发送消费者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsSendConsumer {

    private final MessageIdempotentService messageIdempotentService;
    private final SmsSender smsSender;

    @RabbitListener(queues = RabbitMQConfig.SMS_SEND_QUEUE)
    public void handleSmsSend(String body) {
        Map<String, Object> payload = JsonUtils.fromJson(body, new TypeReference<Map<String, Object>>() {
        });
        String scene = requireNonBlank(payload, "scene", body);
        String phone = requireNonBlank(payload, "phone", body);
        String code = requireNonBlank(payload, "code", body);
        String messageId = RabbitMQConfig.SMS_SEND_QUEUE + ":" + scene + ":" + phone + ":" + code;

        if (messageIdempotentService.isProcessed(messageId, RabbitMQConfig.SMS_SEND_QUEUE)) {
            log.info("短信发送消息已处理,跳过 messageId={}", messageId);
            return;
        }

        try {
            smsSender.send(phone, code);
            messageIdempotentService.recordSuccess(
                    messageId,
                    RabbitMQConfig.SMS_SEND_QUEUE,
                    body,
                    SmsSendConsumer.class.getSimpleName());
        } catch (Exception e) {
            messageIdempotentService.recordFailure(
                    messageId,
                    RabbitMQConfig.SMS_SEND_QUEUE,
                    body,
                    SmsSendConsumer.class.getSimpleName(),
                    e.getMessage());
            throw e;
        }
    }

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
