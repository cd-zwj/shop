package com.payment.consumer;

import com.payment.service.MessageClaim;
import com.payment.service.MessageClaimResult;
import com.payment.service.MessageIdempotentService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/** 将数据库抢占结果转换为 RabbitMQ 消费确认行为。 */
final class MessageClaimGuard {

    private MessageClaimGuard() {
    }

    static String acquire(MessageIdempotentService service,
                          String messageId,
                          String queueName,
                          String messageBody,
                          String consumerName) {
        MessageClaim claim = service.tryClaim(
                messageId, queueName, messageBody, consumerName);
        if (claim.result() == MessageClaimResult.ACQUIRED) {
            return claim.token();
        }
        if (claim.result() == MessageClaimResult.IN_PROGRESS) {
            throw new AmqpRejectAndDontRequeueException("消息正在由其他线程处理: " + messageId);
        }
        return null;
    }
}
