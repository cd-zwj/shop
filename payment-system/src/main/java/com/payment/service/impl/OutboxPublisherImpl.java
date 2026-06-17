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

@Service
@RequiredArgsConstructor
public class OutboxPublisherImpl implements OutboxPublisher {

    private static final String DEFAULT_MESSAGE_PREFIX = "MSG";

    private final MessageOutboxMapper messageOutboxMapper;

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
