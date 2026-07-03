package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息幂等性记录实体。
 * 对应 message_idempotent 表，用于保障消息消费的幂等性。
 * 消费者在处理消息前先查询此表，若已存在记录则跳过，防止重复消费导致数据不一致。
 */
@Data
@TableName("message_idempotent")
public class MessageIdempotent {

    /**
     * 消息唯一ID（主键）
     */
    @TableId(type = IdType.INPUT)
    private String messageId;

    /**
     * 队列名称
     */
    private String queueName;

    /**
     * 消息内容
     */
    private String messageBody;

    /**
     * 消费者名称
     */
    private String consumerName;

    /**
     * 处理状态：1-处理成功，2-处理失败
     */
    private Integer status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
