package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.entity.MessageOutbox;
import com.payment.enums.OutboxSendStatusEnum;
import com.payment.mapper.MessageOutboxMapper;
import com.payment.service.OutboxPublisher;
import com.payment.service.outbox.OutboxMessageCommand;
import com.payment.util.BizNoGenerator;
import com.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Outbox 消息发布器实现类。
 * <p>
 * 基于 Outbox 模式（Transactional Outbox）实现可靠消息投递。将待发送的业务消息
 * 先持久化到 {@code message_outbox} 表，再由后台调度器异步投递至 RabbitMQ，
 * 保证消息与业务数据在同一事务中原子写入，解决分布式环境下的消息丢失问题。
 * <p>
 * 消息初始状态为 PENDING，由 {@code OutboxScheduler} 定时扫描并投递。
 *
 * @see com.payment.service.OutboxPublisher
 * @see com.payment.service.outbox.OutboxMessageCommand
 */
@Service
@RequiredArgsConstructor
public class OutboxPublisherImpl implements OutboxPublisher {

    private static final String DEFAULT_MESSAGE_PREFIX = "MSG";

    private final MessageOutboxMapper messageOutboxMapper;

    /**
     * 发布 Outbox 消息。
     * <p>
     * 校验命令参数合法性后，生成唯一消息 ID，序列化消息体为 JSON，
     * 插入 {@code message_outbox} 记录（状态 PENDING）。
     *
     * @param command Outbox 消息命令，包含业务类型、路由键、消息体等
     * @return 已持久化的 Outbox 消息实体
     * @throws BusinessException 命令参数不合法时抛出
     */
    @Override
    public MessageOutbox publish(OutboxMessageCommand command) {
        validate(command);
        MessageOutbox outbox = new MessageOutbox();
        outbox.setMessageId(BizNoGenerator.generate(firstNonBlank(command.getMessagePrefix(), DEFAULT_MESSAGE_PREFIX)));
        outbox.setBizType(command.getBizType());
        outbox.setBizNo(command.getBizNo());
        outbox.setExchangeName(command.getExchangeName() == null ? "" : command.getExchangeName());
        outbox.setRoutingKey(command.getRoutingKey());
        outbox.setMessageBody(JsonUtils.toJson(command.getMessageBody()));
        outbox.setSendStatus(OutboxSendStatusEnum.PENDING.name());
        outbox.setRetryCount(0);
        outbox.setNextRetryTime(LocalDateTime.now());
        messageOutboxMapper.insert(outbox);
        return outbox;
    }

    /**
     * 校验 Outbox 消息命令参数。
     *
     * @param command 待校验的命令
     * @throws BusinessException 任一必要参数缺失时抛出
     */
    private void validate(OutboxMessageCommand command) {
        if (command == null) {
            throw new BusinessException("Outbox message command cannot be null");
        }
        if (isBlank(command.getBizType())) {
            throw new BusinessException("Outbox bizType cannot be blank");
        }
        if (isBlank(command.getBizNo())) {
            throw new BusinessException("Outbox bizNo cannot be blank");
        }
        if (isBlank(command.getRoutingKey())) {
            throw new BusinessException("Outbox routingKey cannot be blank");
        }
        if (command.getMessageBody() == null) {
            throw new BusinessException("Outbox messageBody cannot be null");
        }
    }

    private String firstNonBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
