package com.payment.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.config.RabbitMQConfig;
import com.payment.entity.DeadLetterTask;
import com.payment.entity.RechargeOrder;
import com.payment.mapper.DeadLetterTaskMapper;
import com.payment.mapper.RechargeOrderMapper;
import com.payment.util.JsonUtils;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterConsumer {

    private final RechargeOrderMapper rechargeOrderMapper;
    private final DeadLetterTaskMapper deadLetterTaskMapper;

    @RabbitListener(queues = "payment.dlx.queue", ackMode = "MANUAL")
    public void handleDeadLetter(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);

            String originalQueue = resolveOriginalQueue(message);
            if (RabbitMQConfig.RECHARGE_ORDER_DELAY_QUEUE.equals(originalQueue)) {
                log.info("收到充值订单超时消息：{}", messageBody);
                handleRechargeOrderExpiration(stripQuotes(messageBody));
                channel.basicAck(deliveryTag, false);
                return;
            }

            persistDeadLetter(message, messageBody, originalQueue, null);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理死信消息失败", e);
            safeAck(channel, deliveryTag);
        }
    }

    private String resolveOriginalQueue(Message message) {
        List<Map<String, ?>> xDeathHeader = message.getMessageProperties().getXDeathHeader();
        if (xDeathHeader != null && !xDeathHeader.isEmpty()) {
            Object queue = xDeathHeader.get(0).get("queue");
            return queue == null ? null : queue.toString();
        }
        return null;
    }

    private void persistDeadLetter(Message message, String messageBody, String originalQueue, String overrideReason) {
        try {
            DeadLetterTask task = new DeadLetterTask();
            task.setMessageId(message.getMessageProperties().getMessageId());
            task.setQueueName(originalQueue);
            task.setExchangeName(message.getMessageProperties().getReceivedExchange());
            task.setRoutingKey(message.getMessageProperties().getReceivedRoutingKey());
            task.setHeadersJson(serializeHeaders(message));
            task.setMessageBody(messageBody);
            task.setFailReason(overrideReason == null ? resolveDeathReason(message) : overrideReason);
            task.setHandleStatus("PENDING");
            task.setRetryCount(0);
            task.setCreateTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            deadLetterTaskMapper.insert(task);
            log.info("死信消息已持久化, originalQueue={}, messageId={}", originalQueue, message.getMessageProperties().getMessageId());
        } catch (Exception e) {
            log.error("死信消息持久化失败, originalQueue={}, messageId={}", originalQueue, message.getMessageProperties().getMessageId(), e);
        }
    }

    private String resolveDeathReason(Message message) {
        String reason = message.getMessageProperties().getHeader("x-first-death-reason");
        if (reason != null) {
            return reason;
        }
        List<Map<String, ?>> xDeathHeader = message.getMessageProperties().getXDeathHeader();
        if (xDeathHeader != null && !xDeathHeader.isEmpty()) {
            Object reasonValue = xDeathHeader.get(0).get("reason");
            return reasonValue == null ? null : reasonValue.toString();
        }
        return null;
    }

    private String serializeHeaders(Message message) {
        try {
            return JsonUtils.toJson(message.getMessageProperties().getHeaders());
        } catch (Exception e) {
            log.warn("Failed to serialize dead letter headers", e);
            return null;
        }
    }

    private void handleRechargeOrderExpiration(String orderNo) {
        try {
            LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RechargeOrder::getOrderNo, orderNo)
                    .eq(RechargeOrder::getDeleted, 0);
            RechargeOrder order = rechargeOrderMapper.selectOne(wrapper);

            if (order != null) {
                if (order.getPayStatus() == 0) {
                    order.setPayStatus(2);
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

    private String stripQuotes(String value) {
        if (value == null) {
            return null;
        }
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private void safeAck(Channel channel, long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("确认死信消息失败", ex);
        }
    }
}
