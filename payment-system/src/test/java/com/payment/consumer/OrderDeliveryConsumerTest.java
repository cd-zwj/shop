package com.payment.consumer;

import com.payment.config.RabbitMQConfig;
import com.payment.service.MessageIdempotentService;
import com.payment.service.delivery.OrderDeliveryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderDeliveryConsumerTest {

    private final OrderDeliveryService orderDeliveryService = mock(OrderDeliveryService.class);
    private final MessageIdempotentService messageIdempotentService = mock(MessageIdempotentService.class);
    private final OrderDeliveryConsumer consumer = new OrderDeliveryConsumer(orderDeliveryService, messageIdempotentService);

    /**
     * H4 回归：bizNo 缺失时必须抛 IllegalArgumentException 让消息进死信队列,
     * 不能被 String.valueOf(null) 偷偷转成 "null" 然后静默处理掉。
     */
    @Test
    void handleDeliveryShouldRejectMissingBizNo() {
        String body = "{\"bizType\":\"ORDER_DELIVERY\"}";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> consumer.handleDelivery(body));
        assert ex.getMessage().contains("bizNo");
        verify(orderDeliveryService, never()).deliverOrder(any());
        verify(messageIdempotentService, never()).recordSuccess(any(), any(), any(), any());
    }

    /**
     * H4 回归：bizNo 为空字符串同样拒绝。
     */
    @Test
    void handleDeliveryShouldRejectBlankBizNo() {
        String body = "{\"bizNo\":\"\",\"bizType\":\"ORDER_DELIVERY\"}";
        assertThrows(IllegalArgumentException.class, () -> consumer.handleDelivery(body));
        verify(orderDeliveryService, never()).deliverOrder(any());
    }

    /**
     * 正常消息能够幂等记账。
     */
    @Test
    void handleDeliveryShouldRecordSuccessWhenDelivered() {
        String body = "{\"bizNo\":\"ORD123\",\"bizType\":\"ORDER_DELIVERY\"}";
        when(messageIdempotentService.isProcessed(any(), any())).thenReturn(false);
        consumer.handleDelivery(body);
        verify(orderDeliveryService).deliverOrder("ORD123");
        verify(messageIdempotentService).recordSuccess(
                RabbitMQConfig.V1_ORDER_DELIVERY_QUEUE + ":ORD123",
                RabbitMQConfig.V1_ORDER_DELIVERY_QUEUE,
                body,
                "OrderDeliveryConsumer");
    }

    /**
     * 已处理过的消息直接跳过。
     */
    @Test
    void handleDeliveryShouldSkipWhenAlreadyProcessed() {
        String body = "{\"bizNo\":\"ORD-DUP\",\"bizType\":\"ORDER_DELIVERY\"}";
        when(messageIdempotentService.isProcessed(any(), any())).thenReturn(true);
        consumer.handleDelivery(body);
        verify(orderDeliveryService, never()).deliverOrder(any());
        verify(messageIdempotentService, never()).recordSuccess(any(), any(), any(), any());
    }
}
