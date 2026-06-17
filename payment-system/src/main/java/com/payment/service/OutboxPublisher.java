package com.payment.service;

import com.payment.entity.MessageOutbox;
import com.payment.service.outbox.OutboxMessageCommand;

public interface OutboxPublisher {

    MessageOutbox publish(OutboxMessageCommand command);
}
