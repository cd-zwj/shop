package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息消费日志实体。
 * 对应 message_consume_log 表，记录每次消息消费的结果。
 */
@Data
@TableName("message_consume_log")
public class MessageConsumeLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息ID */
    private String messageId;

    /** 队列名称 */
    private String queueName;

    /** 消费者名称 */
    private String consumerName;

    /** 业务类型 */
    private String bizType;

    /** 业务单号 */
    private String bizNo;

    /** 消费状态：SUCCESS / FAIL / IGNORED */
    private String consumeStatus;

    /** 错误信息 */
    private String errorMessage;

    /** 消费时间 */
    private LocalDateTime consumeTime;
}
