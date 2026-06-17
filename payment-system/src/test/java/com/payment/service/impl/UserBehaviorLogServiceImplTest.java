package com.payment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.config.RabbitMQConfig;
import com.payment.entity.UserBehaviorLog;
import com.payment.mapper.UserBehaviorLogMapper;
import com.payment.service.OutboxPublisher;
import com.payment.service.outbox.OutboxMessageCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserBehaviorLogServiceImplTest {

    @Test
    void recordBehaviorShouldPersistLogAndPublishOutboxEvent() {
        UserBehaviorLogMapper behaviorLogMapper = mock(UserBehaviorLogMapper.class);
        OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
        UserBehaviorLogServiceImpl service = new UserBehaviorLogServiceImpl(
                behaviorLogMapper,
                new ObjectMapper(),
                outboxPublisher);
        when(behaviorLogMapper.insert(any(UserBehaviorLog.class))).thenAnswer(invocation -> {
            UserBehaviorLog log = invocation.getArgument(0);
            log.setId(7001L);
            return 1;
        });

        service.recordBehavior(42L, 10L, "VIEW", "PRODUCT", 501L, "查看商品详情");

        ArgumentCaptor<UserBehaviorLog> logCaptor = ArgumentCaptor.forClass(UserBehaviorLog.class);
        verify(behaviorLogMapper).insert(logCaptor.capture());
        UserBehaviorLog saved = logCaptor.getValue();
        assertEquals(42L, saved.getUserId());
        assertEquals(10L, saved.getTenantId());
        assertEquals("VIEW", saved.getBehaviorType());

        ArgumentCaptor<OutboxMessageCommand> eventCaptor = ArgumentCaptor.forClass(OutboxMessageCommand.class);
        verify(outboxPublisher).publish(eventCaptor.capture());
        OutboxMessageCommand command = eventCaptor.getValue();
        assertEquals("BHV", command.getMessagePrefix());
        assertEquals("USER_BEHAVIOR", command.getBizType());
        assertEquals("USER_BEHAVIOR_7001", command.getBizNo());
        assertEquals(RabbitMQConfig.USER_BEHAVIOR_QUEUE, command.getRoutingKey());

        Map<String, Object> body = (Map<String, Object>) command.getMessageBody();
        assertEquals("USER_BEHAVIOR", body.get("bizType"));
        assertEquals(7001L, body.get("behaviorLogId"));
        assertEquals(42L, body.get("platformUserId"));
        assertEquals(10L, body.get("tenantId"));
        assertEquals("VIEW", body.get("behaviorType"));
        assertEquals("PRODUCT", body.get("targetType"));
        assertEquals(501L, body.get("targetId"));
        assertEquals("查看商品详情", body.get("detail"));
    }

    @Test
    void recordBehaviorShouldDefaultNullTenantToZeroInOutboxEvent() {
        UserBehaviorLogMapper behaviorLogMapper = mock(UserBehaviorLogMapper.class);
        OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
        UserBehaviorLogServiceImpl service = new UserBehaviorLogServiceImpl(
                behaviorLogMapper,
                new ObjectMapper(),
                outboxPublisher);
        when(behaviorLogMapper.insert(any(UserBehaviorLog.class))).thenAnswer(invocation -> {
            UserBehaviorLog log = invocation.getArgument(0);
            log.setId(7002L);
            return 1;
        });

        service.recordBehavior(42L, null, "SEARCH", null, null, "关键词");

        ArgumentCaptor<OutboxMessageCommand> eventCaptor = ArgumentCaptor.forClass(OutboxMessageCommand.class);
        verify(outboxPublisher).publish(eventCaptor.capture());
        Map<String, Object> body = (Map<String, Object>) eventCaptor.getValue().getMessageBody();
        assertEquals(0L, body.get("tenantId"));
        assertEquals("关键词", body.get("detail"));
    }
}
