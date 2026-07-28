package com.payment.consumer;

import com.payment.config.RabbitMQConfig;
import com.payment.service.MessageIdempotentService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.payment.service.MessageClaim.acquired;
import static com.payment.service.MessageClaim.completed;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserNotificationConsumerTest {

    private final MessageIdempotentService messageIdempotentService = mock(MessageIdempotentService.class);
    private final UserNotificationConsumer consumer = new UserNotificationConsumer(messageIdempotentService);

    @Test
    void handleNotificationShouldRejectMissingNotificationId() {
        String body = "{\"bizType\":\"USER_NOTIFICATION\",\"platformUserId\":100}";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> consumer.handleNotification(body));

        assert ex.getMessage().contains("notificationId");
        verify(messageIdempotentService, never()).recordSuccess(any(), any(), any(), any(), any());
    }

    @Test
    void handleNotificationShouldSkipProcessedMessage() {
        String body = "{\"notificationId\":7,\"platformUserId\":100,\"category\":\"ORDER\"}";
        String messageId = RabbitMQConfig.USER_NOTIFICATION_QUEUE + ":7";
        when(messageIdempotentService.tryClaim(
                messageId, RabbitMQConfig.USER_NOTIFICATION_QUEUE, body,
                UserNotificationConsumer.class.getSimpleName())).thenReturn(completed());

        consumer.handleNotification(body);

        verify(messageIdempotentService, never()).recordSuccess(any(), any(), any(), any(), any());
    }

    @Test
    void handleNotificationShouldRecordSuccess() {
        String body = "{\"notificationId\":7,\"platformUserId\":100,\"category\":\"ORDER\"}";
        String messageId = RabbitMQConfig.USER_NOTIFICATION_QUEUE + ":7";
        when(messageIdempotentService.tryClaim(
                messageId, RabbitMQConfig.USER_NOTIFICATION_QUEUE, body,
                UserNotificationConsumer.class.getSimpleName())).thenReturn(acquired("claim-token"));

        consumer.handleNotification(body);

        verify(messageIdempotentService).recordSuccess(
                messageId,
                RabbitMQConfig.USER_NOTIFICATION_QUEUE,
                body,
                UserNotificationConsumer.class.getSimpleName(),
                "claim-token");
    }

    @Test
    void handleNotificationShouldRecordFailureAndRethrow() {
        String body = "{\"notificationId\":7,\"platformUserId\":100,\"category\":\"ORDER\"}";
        String messageId = RabbitMQConfig.USER_NOTIFICATION_QUEUE + ":7";
        UserNotificationConsumer failingConsumer = new UserNotificationConsumer(messageIdempotentService) {
            @Override
            protected void processNotificationEvent(Map<String, Object> payload) {
                throw new IllegalStateException("push failed");
            }
        };
        when(messageIdempotentService.tryClaim(
                messageId, RabbitMQConfig.USER_NOTIFICATION_QUEUE, body,
                UserNotificationConsumer.class.getSimpleName())).thenReturn(acquired("claim-token"));

        assertThrows(IllegalStateException.class, () -> failingConsumer.handleNotification(body));

        verify(messageIdempotentService).recordFailure(
                messageId,
                RabbitMQConfig.USER_NOTIFICATION_QUEUE,
                body,
                UserNotificationConsumer.class.getSimpleName(),
                "claim-token",
                "push failed");
    }
}
