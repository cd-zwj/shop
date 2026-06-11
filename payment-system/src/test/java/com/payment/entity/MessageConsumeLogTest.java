package com.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试 — MessageConsumeLog 实体字段映射与 getter/setter 正确性。
 */
class MessageConsumeLogTest {

    @Test
    void allFields_shouldBeSettableAndRetrievable() {
        // Arrange
        MessageConsumeLog log = new MessageConsumeLog();
        LocalDateTime consumeTime = LocalDateTime.of(2026, 6, 10, 14, 30, 0);

        // Act
        log.setId(1L);
        log.setMessageId("MSG-001");
        log.setQueueName("payment.callback.queue");
        log.setConsumerName("PaymentCallbackConsumer");
        log.setBizType("ORDER");
        log.setBizNo("ORD-20260610-001");
        log.setConsumeStatus("SUCCESS");
        log.setErrorMessage(null);
        log.setConsumeTime(consumeTime);

        // Assert
        assertEquals(1L, log.getId());
        assertEquals("MSG-001", log.getMessageId());
        assertEquals("payment.callback.queue", log.getQueueName());
        assertEquals("PaymentCallbackConsumer", log.getConsumerName());
        assertEquals("ORDER", log.getBizType());
        assertEquals("ORD-20260610-001", log.getBizNo());
        assertEquals("SUCCESS", log.getConsumeStatus());
        assertNull(log.getErrorMessage());
        assertEquals(consumeTime, log.getConsumeTime());
    }

    @Test
    void defaultFields_shouldBeNull_whenNewInstanceCreated() {
        // Arrange & Act
        MessageConsumeLog log = new MessageConsumeLog();

        // Assert
        assertNull(log.getId());
        assertNull(log.getMessageId());
        assertNull(log.getQueueName());
        assertNull(log.getConsumerName());
        assertNull(log.getBizType());
        assertNull(log.getBizNo());
        assertNull(log.getConsumeStatus());
        assertNull(log.getErrorMessage());
        assertNull(log.getConsumeTime());
    }

    @Test
    void tableNameAnnotation_shouldBeMessageConsumeLog() {
        // Arrange & Act
        TableName annotation = MessageConsumeLog.class.getAnnotation(TableName.class);

        // Assert
        assertNotNull(annotation);
        assertEquals("message_consume_log", annotation.value());
    }

    @Test
    void consumeStatus_shouldSupportAllValidValues() {
        // Arrange
        MessageConsumeLog log = new MessageConsumeLog();
        String[] statuses = {"SUCCESS", "FAIL", "IGNORED"};

        // Act & Assert
        for (String status : statuses) {
            log.setConsumeStatus(status);
            assertEquals(status, log.getConsumeStatus());
        }
    }

    @Test
    void errorMessage_shouldStoreLongText() {
        // Arrange - DDL: error_message VARCHAR(500)
        MessageConsumeLog log = new MessageConsumeLog();
        String longError = "E".repeat(500);

        // Act
        log.setErrorMessage(longError);

        // Assert
        assertEquals(longError, log.getErrorMessage());
        assertEquals(500, log.getErrorMessage().length());
    }

    @Test
    void serializable_shouldBeImplemented() {
        // Arrange & Act
        boolean isSerializable = java.io.Serializable.class.isAssignableFrom(MessageConsumeLog.class);

        // Assert
        assertTrue(isSerializable);
    }
}
