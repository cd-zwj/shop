package com.payment.enums;

/**
 * Outbox 消息发送状态枚举。
 *
 * 描述 Outbox 模式中待发消息的投递状态，
 * 用于保障本地事务与消息发送的一致性。
 */
public enum OutboxSendStatusEnum {
    /** 待发送：消息已写入 outbox 表，等待定时任务扫描投递 */
    PENDING,
    /** 已发送：消息已成功投递到 RabbitMQ */
    SENT,
    /** 发送失败：投递到 MQ 失败，等待重试 */
    FAILED,
    /** 已死信：超过最大重试次数仍未成功，转为死信消息待人工处理 */
    DEAD
}
