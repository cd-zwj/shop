package com.payment.enums;

/**
 * 消息处理状态枚举。
 *
 * 描述 RabbitMQ 消费者处理消息的状态，
 * 用于消息消费的幂等控制和失败重试。
 */
public enum MessageProcessStatusEnum {
    /** 待处理：消息已接收到队列，等待消费者处理 */
    PENDING,
    /** 处理成功：消息已被成功消费和处理 */
    SUCCESS,
    /** 处理失败：消息消费失败，进入重试或死信队列 */
    FAILED
}
