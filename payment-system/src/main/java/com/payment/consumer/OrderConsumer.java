package com.payment.consumer;

import com.payment.util.JsonUtils;
import com.payment.entity.PaymentOrder;
import com.payment.service.MessageIdempotentService;
import com.payment.service.PaymentOrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单消息消费者
 * 处理订单创建和支付成功的消息
 */
@Slf4j
@Component
public class OrderConsumer {
    
    @Autowired
    private MessageIdempotentService messageIdempotentService;
    
    @Autowired
    private PaymentOrderService paymentOrderService;
    
    private static final String CONSUMER_ORDER_CREATED = "OrderCreatedConsumer";
    private static final String CONSUMER_ORDER_PAID = "OrderPaidConsumer";
    private static final String QUEUE_ORDER_CREATED = "payment.order.created";
    private static final String QUEUE_ORDER_PAID = "payment.order.paid";
    
    /**
     * 消费订单创建消息
     * 处理订单创建后的业务逻辑（如发送通知、更新统计等）
     */
    @RabbitListener(queues = QUEUE_ORDER_CREATED, ackMode = "MANUAL")
    public void handleOrderCreated(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageBody = null;
        String messageId = null;
        
        try {
            messageBody = new String(message.getBody());
            log.info("收到订单创建消息：{}", messageBody);
            
            // 解析消息
            Map<String, Object> messageMap = JsonUtils.fromJson(messageBody, Map.class);
            messageId = (String) messageMap.get("messageId");
            String orderNo = (String) messageMap.get("orderNo");
            
            if (messageId == null) {
                log.error("消息ID为空，无法进行幂等性检查");
                channel.basicNack(deliveryTag, false, false);
                return;
            }
            
            // 幂等性检查
            if (messageIdempotentService.isProcessed(messageId, QUEUE_ORDER_CREATED)) {
                log.info("消息已处理过，跳过。messageId: {}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // 处理订单创建业务逻辑
            processOrderCreated(orderNo, messageMap);
            
            // 记录处理成功
            messageIdempotentService.recordSuccess(messageId, QUEUE_ORDER_CREATED, messageBody, CONSUMER_ORDER_CREATED);
            
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("订单创建消息处理成功，orderNo: {}", orderNo);
            
        } catch (Exception e) {
            log.error("处理订单创建消息失败，messageId: {}", messageId, e);
            try {
                // 记录处理失败
                if (messageId != null) {
                    messageIdempotentService.recordFailure(messageId, QUEUE_ORDER_CREATED, messageBody, 
                            CONSUMER_ORDER_CREATED, e.getMessage());
                }
                
                // 拒绝消息，不重新入队，进入死信队列
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("拒绝消息失败", ex);
            }
        }
    }
    
    /**
     * 消费订单支付成功消息
     * 处理订单支付成功后的业务逻辑（如发送通知、更新库存、积分奖励等）
     */
    @RabbitListener(queues = QUEUE_ORDER_PAID, ackMode = "MANUAL")
    public void handleOrderPaid(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageBody = null;
        String messageId = null;
        
        try {
            messageBody = new String(message.getBody());
            log.info("收到订单支付成功消息：{}", messageBody);
            
            // 解析消息
            Map<String, Object> messageMap = JsonUtils.fromJson(messageBody, Map.class);
            messageId = (String) messageMap.get("messageId");
            String orderNo = (String) messageMap.get("orderNo");
            
            if (messageId == null) {
                log.error("消息ID为空，无法进行幂等性检查");
                channel.basicNack(deliveryTag, false, false);
                return;
            }
            
            // 幂等性检查
            if (messageIdempotentService.isProcessed(messageId, QUEUE_ORDER_PAID)) {
                log.info("消息已处理过，跳过。messageId: {}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // 处理订单支付成功业务逻辑
            processOrderPaid(orderNo, messageMap);
            
            // 记录处理成功
            messageIdempotentService.recordSuccess(messageId, QUEUE_ORDER_PAID, messageBody, CONSUMER_ORDER_PAID);
            
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("订单支付成功消息处理成功，orderNo: {}", orderNo);
            
        } catch (Exception e) {
            log.error("处理订单支付成功消息失败，messageId: {}", messageId, e);
            try {
                // 记录处理失败
                if (messageId != null) {
                    messageIdempotentService.recordFailure(messageId, QUEUE_ORDER_PAID, messageBody, 
                            CONSUMER_ORDER_PAID, e.getMessage());
                }
                
                // 拒绝消息，不重新入队，进入死信队列
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("拒绝消息失败", ex);
            }
        }
    }
    
    /**
     * 处理订单创建业务逻辑
     * 
     * @param orderNo 订单号
     * @param messageMap 消息内容
     */
    private void processOrderCreated(String orderNo, Map<String, Object> messageMap) {
        // 查询订单信息
        PaymentOrder order = paymentOrderService.getOrderByNo(orderNo);
        if (order == null) {
            log.warn("订单不存在，orderNo: {}", orderNo);
            return;
        }
        
        // TODO: 实现订单创建后的业务逻辑
        // 1. 发送订单创建通知（短信、邮件、站内信等）
        // 2. 更新商品销量统计
        // 3. 记录用户行为日志
        // 4. 触发其他业务流程
        
        log.info("订单创建业务处理完成，orderNo: {}, totalAmount: {}", orderNo, order.getAmount());
    }
    
    /**
     * 处理订单支付成功业务逻辑
     * 
     * @param orderNo 订单号
     * @param messageMap 消息内容
     */
    private void processOrderPaid(String orderNo, Map<String, Object> messageMap) {
        // 查询订单信息
        PaymentOrder order = paymentOrderService.getOrderByNo(orderNo);
        if (order == null) {
            log.warn("订单不存在，orderNo: {}", orderNo);
            return;
        }
        
        // TODO: 实现订单支付成功后的业务逻辑
        // 1. 发送支付成功通知（短信、邮件、站内信等）
        // 2. 更新库存（如果是实物商品）
        // 3. 发放积分奖励
        // 4. 更新商户余额
        // 5. 触发发货流程（如果是实物商品）
        // 6. 记录用户行为日志
        
        log.info("订单支付成功业务处理完成，orderNo: {}, totalAmount: {}", orderNo, order.getAmount());
    }
}


