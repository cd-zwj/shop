package com.payment.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.payment.config.RabbitMQConfig;
import com.payment.service.MessageIdempotentService;
import com.payment.service.delivery.OrderDeliveryService;
import com.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单交付队列消费者
 * <p>
 * 从 {@link RabbitMQConfig#V1_ORDER_DELIVERY_QUEUE} 队列中消费订单交付事件。
 * 与 {@link PaymentV1Consumer#handleOrderPaid} 解耦：支付回调链路只负责"已支付"，
 * 交付链路独立异步执行，失败可以走死信队列重试而不污染支付主流程。
 * 通过 {@link MessageIdempotentService} 保障消息幂等性。
 * </p>
 *
 * @author payment-system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDeliveryConsumer {

    /**
     * 订单交付服务，执行具体的交付逻辑
     */
    private final OrderDeliveryService orderDeliveryService;

    /**
     * 消息幂等服务，用于防止重复消费
     */
    private final MessageIdempotentService messageIdempotentService;

    /**
     * 处理订单交付消息
     * <p>
     * 从消息体中解析订单编号，进行必填校验和幂等校验后，
     * 调用交付服务执行商品交付。畸形消息（缺少 bizNo）会抛出异常，
     * 由 Spring AMQP 将其送入死信队列。
     * </p>
     *
     * @param body 消息体 JSON 字符串，必须包含 bizNo 字段
     * @throws IllegalArgumentException 当消息体缺少 bizNo 或 bizNo 为空白时抛出
     */
    @RabbitListener(queues = RabbitMQConfig.V1_ORDER_DELIVERY_QUEUE)
    public void handleDelivery(String body) {
        Map<String, Object> payload = JsonUtils.fromJson(body, new TypeReference<Map<String, Object>>() {
        });

        // 防御 String.valueOf(null) 把 null 变成字符串 "null" 的老坑 ——
        // 畸形消息必须抛异常,让 Spring AMQP 把它送到死信队列,而不是被静默"消费成功"。
        Object rawBizNo = payload == null ? null : payload.get("bizNo");
        if (rawBizNo == null) {
            throw new IllegalArgumentException("订单交付消息缺少 bizNo, body=" + body);
        }
        String orderNo = rawBizNo.toString();
        if (orderNo.isBlank()) {
            throw new IllegalArgumentException("订单交付消息 bizNo 为空, body=" + body);
        }

        String messageId = RabbitMQConfig.V1_ORDER_DELIVERY_QUEUE + ":" + orderNo;

        if (messageIdempotentService.isProcessed(messageId, RabbitMQConfig.V1_ORDER_DELIVERY_QUEUE)) {
            log.info("订单交付消息已处理,跳过 messageId={}", messageId);
            return;
        }

        try {
            orderDeliveryService.deliverOrder(orderNo);
            messageIdempotentService.recordSuccess(
                    messageId,
                    RabbitMQConfig.V1_ORDER_DELIVERY_QUEUE,
                    body,
                    OrderDeliveryConsumer.class.getSimpleName());
        } catch (Exception e) {
            messageIdempotentService.recordFailure(
                    messageId,
                    RabbitMQConfig.V1_ORDER_DELIVERY_QUEUE,
                    body,
                    OrderDeliveryConsumer.class.getSimpleName(),
                    e.getMessage());
            throw e;
        }
    }
}
