package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 死信任务实体。
 * 对应 dead_letter_task 表，存储进入死信队列的消息记录。
 * 当消息经过多次重试仍无法成功消费时，会被路由到死信队列，
 * 由系统记录于此表中，等待人工介入或自动补偿处理。
 */
@Data
@TableName("dead_letter_task")
public class DeadLetterTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原始消息唯一标识 */
    private String messageId;

    /** 原始队列名称 */
    private String queueName;

    /** 原始交换机名称 */
    private String exchangeName;

    /** 原始路由键 */
    private String routingKey;

    /** 原始消息头信息（JSON 格式） */
    private String headersJson;

    /** 原始消息体（JSON 格式） */
    private String messageBody;

    /** 失败原因描述 */
    private String failReason;

    /** 处理状态：PENDING-待处理、HANDLED-已处理、IGNORED-已忽略 */
    private String handleStatus;

    /** 累计重试次数 */
    private Integer retryCount;

    /** 最后一次处理时间 */
    private LocalDateTime lastHandleTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
