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

class PointsEventConsumerTest {

    private final MessageIdempotentService messageIdempotentService = mock(MessageIdempotentService.class);
    private final PointsEventConsumer consumer = new PointsEventConsumer(messageIdempotentService);

    @Test
    void handlePointsEventShouldRejectMissingBizNo() {
        String body = "{\"eventType\":\"POINTS_GRANTED\",\"bizType\":\"ORDER_REWARD\","
                + "\"tenantId\":9,\"platformUserId\":100}";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> consumer.handlePointsEvent(body));

        assert ex.getMessage().contains("bizNo");
        verify(messageIdempotentService, never()).recordSuccess(any(), any(), any(), any(), any());
    }

    @Test
    void handlePointsEventShouldSkipProcessedMessage() {
        String body = "{\"eventType\":\"POINTS_GRANTED\",\"bizType\":\"ORDER_REWARD\",\"bizNo\":\"SO1002\","
                + "\"tenantId\":9,\"platformUserId\":100,\"changePoints\":100}";
        String messageId = RabbitMQConfig.POINTS_EVENT_QUEUE + ":POINTS_GRANTED:ORDER_REWARD:SO1002";
        when(messageIdempotentService.tryClaim(
                messageId, RabbitMQConfig.POINTS_EVENT_QUEUE, body,
                PointsEventConsumer.class.getSimpleName())).thenReturn(completed());

        consumer.handlePointsEvent(body);

        verify(messageIdempotentService, never()).recordSuccess(any(), any(), any(), any(), any());
    }

    @Test
    void handlePointsEventShouldRecordSuccess() {
        String body = "{\"eventType\":\"POINTS_GRANTED\",\"bizType\":\"ORDER_REWARD\",\"bizNo\":\"SO1002\","
                + "\"tenantId\":9,\"platformUserId\":100,\"changePoints\":100}";
        String messageId = RabbitMQConfig.POINTS_EVENT_QUEUE + ":POINTS_GRANTED:ORDER_REWARD:SO1002";
        when(messageIdempotentService.tryClaim(
                messageId, RabbitMQConfig.POINTS_EVENT_QUEUE, body,
                PointsEventConsumer.class.getSimpleName())).thenReturn(acquired("claim-token"));

        consumer.handlePointsEvent(body);

        verify(messageIdempotentService).recordSuccess(
                messageId,
                RabbitMQConfig.POINTS_EVENT_QUEUE,
                body,
                PointsEventConsumer.class.getSimpleName(),
                "claim-token");
    }

    @Test
    void handlePointsEventShouldRecordFailureAndRethrow() {
        String body = "{\"eventType\":\"POINTS_GRANTED\",\"bizType\":\"ORDER_REWARD\",\"bizNo\":\"SO1002\","
                + "\"tenantId\":9,\"platformUserId\":100,\"changePoints\":100}";
        String messageId = RabbitMQConfig.POINTS_EVENT_QUEUE + ":POINTS_GRANTED:ORDER_REWARD:SO1002";
        PointsEventConsumer failingConsumer = new PointsEventConsumer(messageIdempotentService) {
            @Override
            protected void processPointsEvent(Map<String, Object> payload) {
                throw new IllegalStateException("points downstream failed");
            }
        };
        when(messageIdempotentService.tryClaim(
                messageId, RabbitMQConfig.POINTS_EVENT_QUEUE, body,
                PointsEventConsumer.class.getSimpleName())).thenReturn(acquired("claim-token"));

        assertThrows(IllegalStateException.class, () -> failingConsumer.handlePointsEvent(body));

        verify(messageIdempotentService).recordFailure(
                messageId,
                RabbitMQConfig.POINTS_EVENT_QUEUE,
                body,
                PointsEventConsumer.class.getSimpleName(),
                "claim-token",
                "points downstream failed");
    }
}
