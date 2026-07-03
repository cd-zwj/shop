package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息发件箱实体。
 * 对应 message_outbox 表，实现 Outbox 模式保障消息可靠投递。
 * 业务操作与消息发送解耦：业务数据变更时先将消息写入发件箱，
 * 再由定时任务扫描并发送至 RabbitMQ，避免因 MQ 故障导致消息丢失。
 * 支持指数退避重试，超过最大重试次数后标记为 DEAD 进入死信。
 */
@Data
@TableName("message_outbox")
public class MessageOutbox implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息唯一标识，用于幂等和追踪 */
    private String messageId;

    /** 业务类型，如 PAYMENT_CALLBACK、ORDER_STATUS_CHANGE 等 */
    private String bizType;

    /** 业务单号，关联具体的业务记录 */
    private String bizNo;

    /** RabbitMQ 交换机名称 */
    private String exchangeName;

    /** RabbitMQ 路由键 */
    private String routingKey;

    /** 消息体（JSON 格式） */
    private String messageBody;

    /** 发送状态：INIT-待发送、SENT-已发送、FAILED-发送失败、DEAD-已死亡（超过最大重试次数） */
    private String sendStatus;

    /** 已重试次数，初始为 0 */
    private Integer retryCount;

    /** 下次重试时间，采用指数退避策略计算 */
    private LocalDateTime nextRetryTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
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
