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

class CouponEventConsumerTest {

    private final MessageIdempotentService messageIdempotentService = mock(MessageIdempotentService.class);
    private final CouponEventConsumer consumer = new CouponEventConsumer(messageIdempotentService);

    @Test
    void handleCouponEventShouldRejectMissingUserCouponId() {
        String body = "{\"bizType\":\"COUPON_EVENT\",\"eventType\":\"EXPIRED\",\"bizNo\":\"COUPON_EXPIRE_SCAN\"}";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> consumer.handleCouponEvent(body));

        assert ex.getMessage().contains("userCouponId");
        verify(messageIdempotentService, never()).recordSuccess(any(), any(), any(), any());
    }

    @Test
    void handleCouponEventShouldUseEventTypeUserCouponIdAndBizNoAsMessageId() {
        String body = "{\"bizType\":\"COUPON_EVENT\",\"eventType\":\"EXPIRED\",\"bizNo\":\"COUPON_EXPIRE_SCAN\","
                + "\"userCouponId\":501,\"couponStatus\":\"EXPIRED\"}";
        String messageId = RabbitMQConfig.COUPON_EVENT_QUEUE + ":EXPIRED:501:COUPON_EXPIRE_SCAN";
        when(messageIdempotentService.isProcessed(messageId, RabbitMQConfig.COUPON_EVENT_QUEUE))
                .thenReturn(false);

        consumer.handleCouponEvent(body);

        verify(messageIdempotentService).recordSuccess(
                messageId,
                RabbitMQConfig.COUPON_EVENT_QUEUE,
                body,
                CouponEventConsumer.class.getSimpleName());
    }

    @Test
    void handleCouponEventShouldNotTreatSameBizNoDifferentCouponsAsDuplicate() {
        String first = "{\"bizType\":\"COUPON_EVENT\",\"eventType\":\"EXPIRED\",\"bizNo\":\"COUPON_EXPIRE_SCAN\","
                + "\"userCouponId\":501,\"couponStatus\":\"EXPIRED\"}";
        String second = "{\"bizType\":\"COUPON_EVENT\",\"eventType\":\"EXPIRED\",\"bizNo\":\"COUPON_EXPIRE_SCAN\","
                + "\"userCouponId\":502,\"couponStatus\":\"EXPIRED\"}";

        consumer.handleCouponEvent(first);
        consumer.handleCouponEvent(second);

        verify(messageIdempotentService).recordSuccess(
                RabbitMQConfig.COUPON_EVENT_QUEUE + ":EXPIRED:501:COUPON_EXPIRE_SCAN",
                RabbitMQConfig.COUPON_EVENT_QUEUE,
                first,
                CouponEventConsumer.class.getSimpleName());
        verify(messageIdempotentService).recordSuccess(
                RabbitMQConfig.COUPON_EVENT_QUEUE + ":EXPIRED:502:COUPON_EXPIRE_SCAN",
                RabbitMQConfig.COUPON_EVENT_QUEUE,
                second,
                CouponEventConsumer.class.getSimpleName());
    }

    @Test
    void handleCouponEventShouldSkipProcessedMessage() {
        String body = "{\"bizType\":\"COUPON_EVENT\",\"eventType\":\"USED\",\"bizNo\":\"SO1001\","
                + "\"userCouponId\":501,\"couponStatus\":\"USED\"}";
        String messageId = RabbitMQConfig.COUPON_EVENT_QUEUE + ":USED:501:SO1001";
        when(messageIdempotentService.isProcessed(messageId, RabbitMQConfig.COUPON_EVENT_QUEUE))
                .thenReturn(true);

        consumer.handleCouponEvent(body);

        verify(messageIdempotentService, never()).recordSuccess(any(), any(), any(), any());
    }

    @Test
    void handleCouponEventShouldRecordFailureAndRethrow() {
        String body = "{\"bizType\":\"COUPON_EVENT\",\"eventType\":\"USED\",\"bizNo\":\"SO1001\","
                + "\"userCouponId\":501,\"couponStatus\":\"USED\"}";
        String messageId = RabbitMQConfig.COUPON_EVENT_QUEUE + ":USED:501:SO1001";
        CouponEventConsumer failingConsumer = new CouponEventConsumer(messageIdempotentService) {
            @Override
            protected void processCouponEvent(Map<String, Object> payload) {
                throw new IllegalStateException("sync failed");
            }
        };
        when(messageIdempotentService.isProcessed(messageId, RabbitMQConfig.COUPON_EVENT_QUEUE))
                .thenReturn(false);

        assertThrows(IllegalStateException.class, () -> failingConsumer.handleCouponEvent(body));

        verify(messageIdempotentService).recordFailure(
                messageId,
                RabbitMQConfig.COUPON_EVENT_QUEUE,
                body,
                CouponEventConsumer.class.getSimpleName(),
                "sync failed");
    }
}
