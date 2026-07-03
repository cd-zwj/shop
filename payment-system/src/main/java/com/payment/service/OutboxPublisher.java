package com.payment.service;

import com.payment.entity.MessageOutbox;
import com.payment.service.outbox.OutboxMessageCommand;

/**
 * Outbox 消息发布器接口。
 * <p>
 * 基于 Outbox 模式实现可靠消息投递：先将消息写入数据库的 Outbox 表，
 * 再由后台任务异步投递至 RabbitMQ，确保消息不丢失。
 * 适用于支付回调、订单状态变更等需要可靠通知的场景。
 */
public interface OutboxPublisher {

    /**
     * 发布消息到 Outbox。
     * <p>
     * 将消息持久化到 message_outbox 表，由后台调度任务负责实际投递到消息队列。
     *
     * @param command Outbox 消息命令对象，包含 topic、消息体、消息 ID 等信息
     * @return 持久化后的 Outbox 消息实体
     */
    MessageOutbox publish(OutboxMessageCommand command);
}
