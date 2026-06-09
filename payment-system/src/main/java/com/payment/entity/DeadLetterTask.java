package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dead_letter_task")
public class DeadLetterTask implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String messageId;
    private String queueName;
    private String exchangeName;
    private String routingKey;
    private String headersJson;
    private String messageBody;
    private String failReason;
    private String handleStatus;
    private Integer retryCount;
    private LocalDateTime lastHandleTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
