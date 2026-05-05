package com.payment.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.config.RabbitMQConfig;
import com.payment.entity.RechargeOrder;
import com.payment.mapper.RechargeOrderMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 死信队列消费者
 * 处理无法正常消费的消息，记录日志用于人工排查
 */
@Slf4j
@Component
public class DeadLetterConsumer {
    
    @Autowired
    private RechargeOrderMapper rechargeOrderMapper;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 消费死信队列消息
     * 记录详细的消息信息和错误原因，用于人工排查和处理
     */
    @RabbitListener(queues = "payment.dlx.queue", ackMode = "MANUAL")
    public void handleDeadLetter(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            
            // 获取消息的详细信息
            List<Map<String, ?>> xDeathHeader = message.getMessageProperties().getXDeathHeader();
            String originalQueue = null;
            if (xDeathHeader != null && !xDeathHeader.isEmpty()) {
                Map<String, ?> headers = xDeathHeader.get(0);
                originalQueue = (String) headers.get("queue");
            }
            
            // 处理充值订单超时逻辑
            if (RabbitMQConfig.RECHARGE_ORDER_DELAY_QUEUE.equals(originalQueue) || (messageBody.startsWith("R") && messageBody.length() > 10)) {
                log.info("收到充值订单超时消息：{}", messageBody);
                String orderNo = messageBody;
                // 去除可能的引号（如果是JSON字符串）
                if (orderNo.startsWith("\"") && orderNo.endsWith("\"")) {
                    orderNo = orderNo.substring(1, orderNo.length() - 1);
                }
                
                handleRechargeOrderExpiration(orderNo);
                
                // 确认消息
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // 原有的死信记录逻辑
            String originalExchange = message.getMessageProperties().getHeader("x-first-death-exchange");
            String deathReason = message.getMessageProperties().getHeader("x-first-death-reason");
            Long deathTime = message.getMessageProperties().getHeader("x-death-time");
            
            // 记录死信消息的详细信息
            log.error("==================== 死信消息 ====================");
            log.error("接收时间: {}", LocalDateTime.now().format(FORMATTER));
            log.error("原始队列: {}", originalQueue);
            log.error("原始交换机: {}", originalExchange);
            log.error("死亡原因: {}", deathReason);
            log.error("死亡时间: {}", deathTime != null ? LocalDateTime.ofEpochSecond(deathTime / 1000, 0, 
                    java.time.ZoneOffset.ofHours(8)).format(FORMATTER) : "未知");
            log.error("消息ID: {}", message.getMessageProperties().getMessageId());
            log.error("消息内容: {}", messageBody);
            log.error("消息属性: {}", message.getMessageProperties());
            log.error("================================================");
            
            // 手动确认消息（死信队列的消息不再重试）
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("处理死信消息失败", e);
            try {
                // 即使处理失败也要确认，避免死信队列堆积
                channel.basicAck(deliveryTag, false);
            } catch (Exception ex) {
                log.error("确认死信消息失败", ex);
            }
        }
    }
    
    /**
     * 处理充值订单过期逻辑
     */
    private void handleRechargeOrderExpiration(String orderNo) {
        try {
            LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RechargeOrder::getOrderNo, orderNo)
                    .eq(RechargeOrder::getDeleted, 0);
            RechargeOrder order = rechargeOrderMapper.selectOne(wrapper);
            
            if (order != null) {
                if (order.getPayStatus() == 0) { // 待支付
                    order.setPayStatus(2); // 2-已失效/已取消
                    rechargeOrderMapper.updateById(order);
                    log.info("充值订单超时未支付，已自动取消：{}", orderNo);
                } else {
                    log.info("充值订单状态非待支付，忽略超时处理：{}, status={}", orderNo, order.getPayStatus());
                }
            } else {
                log.warn("处理超时充值订单时未找到订单：{}", orderNo);
            }
        } catch (Exception e) {
            log.error("处理充值订单过期失败：{}", orderNo, e);
        }
    }
}
