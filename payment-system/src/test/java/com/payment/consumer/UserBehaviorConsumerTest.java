package com.payment.consumer;

import com.payment.config.RabbitMQConfig;
import com.payment.service.MessageIdempotentService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserBehaviorConsumerTest {

    private final MessageIdempotentService messageIdempotentService = mock(MessageIdempotentService.class);
    private final UserBehaviorConsumer consumer = new UserBehaviorConsumer(messageIdempotentService);

    @Test
    void handleBehaviorShouldRejectMissingBehaviorLogId() {
        String body = "{\"bizType\":\"USER_BEHAVIOR\",\"platformUserId\":100}";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> consumer.handleBehavior(body));

        assert ex.getMessage().contains("behaviorLogId");
        verify(messageIdempotentService, never()).recordSuccess(any(), any(), any(), any());
    }

    @Test
    void handleBehaviorShouldSkipProcessedMessage() {
        String body = "{\"behaviorLogId\":9,\"platformUserId\":100,\"behaviorType\":\"VIEW\"}";
        String messageId = RabbitMQConfig.USER_BEHAVIOR_QUEUE + ":9";
        when(messageIdempotentService.isProcessed(messageId, RabbitMQConfig.USER_BEHAVIOR_QUEUE))
                .thenReturn(true);

        consumer.handleBehavior(body);

        verify(messageIdempotentService, never()).recordSuccess(any(), any(), any(), any());
    }

    @Test
    void handleBehaviorShouldRecordSuccess() {
        String body = "{\"behaviorLogId\":9,\"platformUserId\":100,\"behaviorType\":\"VIEW\"}";
        String messageId = RabbitMQConfig.USER_BEHAVIOR_QUEUE + ":9";
        when(messageIdempotentService.isProcessed(messageId, RabbitMQConfig.USER_BEHAVIOR_QUEUE))
                .thenReturn(false);

        consumer.handleBehavior(body);

        verify(messageIdempotentService).recordSuccess(
                messageId,
                RabbitMQConfig.USER_BEHAVIOR_QUEUE,
                body,
                UserBehaviorConsumer.class.getSimpleName());
    }

    @Test
    void handleBehaviorShouldRecordFailureAndRethrow() {
        String body = "{\"behaviorLogId\":9,\"platformUserId\":100,\"behaviorType\":\"VIEW\"}";
        String messageId = RabbitMQConfig.USER_BEHAVIOR_QUEUE + ":9";
        UserBehaviorConsumer failingConsumer = new UserBehaviorConsumer(messageIdempotentService) {
            @Override
            protected void processBehaviorEvent(Map<String, Object> payload) {
                throw new IllegalStateException("profile failed");
            }
        };
        when(messageIdempotentService.isProcessed(messageId, RabbitMQConfig.USER_BEHAVIOR_QUEUE))
                .thenReturn(false);

        assertThrows(IllegalStateException.class, () -> failingConsumer.handleBehavior(body));

        verify(messageIdempotentService).recordFailure(
                messageId,
                RabbitMQConfig.USER_BEHAVIOR_QUEUE,
                body,
                UserBehaviorConsumer.class.getSimpleName(),
                "profile failed");
    }
}
