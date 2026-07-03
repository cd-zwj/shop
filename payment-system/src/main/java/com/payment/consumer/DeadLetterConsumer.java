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

/**
 * 死信队列消费者
 * <p>
 * 监听死信队列 {@code payment.dlx.queue}，处理所有因重试耗尽而进入死信队列的消息。
 * 主要处理场景：
 * <ul>
 *   <li>充值订单超时未支付 —— 自动将订单状态更新为失败</li>
 *   <li>其他死信消息 —— 持久化到 {@link DeadLetterTask} 表，供后续人工排查或补偿</li>
 * </ul>
 * </p>
 *
 * @author payment-system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterConsumer {

    /**
     * 充值订单 Mapper
     */
    private final RechargeOrderMapper rechargeOrderMapper;

    /**
     * 死信任务 Mapper，用于持久化未识别的死信消息
     */
    private final DeadLetterTaskMapper deadLetterTaskMapper;

    /**
     * 处理死信消息（手动 ACK 模式）
     * <p>
     * 根据消息的原始队列判断处理策略：
     * 来自充值订单延迟队列的消息自动取消超时订单，其余持久化到数据库。
     * </p>
     *
     * @param message RabbitMQ 原始消息对象
     * @param channel RabbitMQ 通道，用于手动确认消息
     */
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

    /**
     * 从消息的 x-death header 中解析原始队列名
     *
     * @param message RabbitMQ 消息对象
     * @return 原始队列名称，若无法解析则返回 null
     */
    private String resolveOriginalQueue(Message message) {
        List<Map<String, ?>> xDeathHeader = message.getMessageProperties().getXDeathHeader();
        if (xDeathHeader != null && !xDeathHeader.isEmpty()) {
            Object queue = xDeathHeader.get(0).get("queue");
            return queue == null ? null : queue.toString();
        }
        return null;
    }

    /**
     * 将死信消息持久化到 {@link DeadLetterTask} 表
     *
     * @param message        RabbitMQ 消息对象
     * @param messageBody    消息体字符串
     * @param originalQueue  原始队列名
     * @param overrideReason 覆盖的失败原因（为 null 时自动从 header 中解析）
     */
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

    /**
     * 从消息 header 中解析死因
     *
     * @param message RabbitMQ 消息对象
     * @return 死因描述字符串，若无法解析则返回 null
     */
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

    /**
     * 将消息 header 序列化为 JSON 字符串
     *
     * @param message RabbitMQ 消息对象
     * @return header 的 JSON 字符串表示，序列化失败时返回 null
     */
    private String serializeHeaders(Message message) {
        try {
            return JsonUtils.toJson(message.getMessageProperties().getHeaders());
        } catch (Exception e) {
            log.warn("Failed to serialize dead letter headers", e);
            return null;
        }
    }

    /**
     * 处理充值订单超时
     * <p>
     * 查询订单状态，若仍为待支付（PENDING），则自动标记为失败（FAIL）。
     * </p>
     *
     * @param orderNo 充值订单编号
     */
    private void handleRechargeOrderExpiration(String orderNo) {
        try {
            LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RechargeOrder::getOrderNo, orderNo);
            RechargeOrder order = rechargeOrderMapper.selectOne(wrapper);

            if (order != null) {
                if ("PENDING".equals(order.getPayStatus())) {
                    order.setPayStatus("FAIL");
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

    /**
     * 去除字符串两端的引号（RabbitMQ 延迟消息可能携带引号包裹的订单号）
     *
     * @param value 原始字符串
     * @return 去除引号后的字符串
     */
    private String stripQuotes(String value) {
        if (value == null) {
            return null;
        }
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * 安全地确认消息（忽略确认过程中的异常）
     *
     * @param channel      RabbitMQ 通道
     * @param deliveryTag  消息投递标签
     */
    private void safeAck(Channel channel, long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("确认死信消息失败", ex);
        }
    }
}
