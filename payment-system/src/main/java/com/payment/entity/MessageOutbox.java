package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("message_outbox")
public class MessageOutbox implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String messageId;
    private String bizType;
    private String bizNo;
    private String exchangeName;
    private String routingKey;
    private String messageBody;
    private String sendStatus;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public MessageOutbox withSendSuccess() {
        MessageOutbox updated = copy();
        updated.setSendStatus(com.payment.enums.OutboxSendStatusEnum.SENT.name());
        updated.setUpdateTime(LocalDateTime.now());
        return updated;
    }

    public MessageOutbox withSendFailure(int maxRetryCount, long retryBaseDelaySeconds) {
        MessageOutbox updated = copy();
        int nextRetryCount = updated.getRetryCount() == null ? 1 : updated.getRetryCount() + 1;
        updated.setRetryCount(nextRetryCount);
        updated.setUpdateTime(LocalDateTime.now());

        if (nextRetryCount >= maxRetryCount) {
            updated.setSendStatus(com.payment.enums.OutboxSendStatusEnum.DEAD.name());
            updated.setNextRetryTime(null);
        } else {
            updated.setSendStatus(com.payment.enums.OutboxSendStatusEnum.FAILED.name());
            updated.setNextRetryTime(LocalDateTime.now().plusSeconds(retryBaseDelaySeconds * (1L << Math.min(nextRetryCount, 6))));
        }
        return updated;
    }

    private MessageOutbox copy() {
        MessageOutbox copy = new MessageOutbox();
        copy.setId(id);
        copy.setMessageId(messageId);
        copy.setBizType(bizType);
        copy.setBizNo(bizNo);
        copy.setExchangeName(exchangeName);
        copy.setRoutingKey(routingKey);
        copy.setMessageBody(messageBody);
        copy.setSendStatus(sendStatus);
        copy.setRetryCount(retryCount);
        copy.setNextRetryTime(nextRetryTime);
        copy.setCreateTime(createTime);
        copy.setUpdateTime(updateTime);
        return copy;
    }
}
