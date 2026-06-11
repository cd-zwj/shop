package com.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试 — RetryTask 实体字段映射与 getter/setter 正确性。
 */
class RetryTaskTest {

    @Test
    void allFields_shouldBeSettableAndRetrievable() {
        // Arrange
        RetryTask task = new RetryTask();
        LocalDateTime now = LocalDateTime.of(2026, 6, 10, 14, 30, 0);
        LocalDateTime nextRetry = LocalDateTime.of(2026, 6, 10, 15, 0, 0);

        // Act
        task.setId(1L);
        task.setTaskNo("TASK-001");
        task.setTaskType("PAYMENT_CALLBACK");
        task.setBizType("ORDER");
        task.setBizNo("ORD-20260610-001");
        task.setMessageId("MSG-001");
        task.setTaskStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetryCount(16);
        task.setNextRetryTime(nextRetry);
        task.setLastErrorMessage("connection timeout");
        task.setExtensionJson("{\"key\":\"value\"}");
        task.setCreateTime(now);
        task.setUpdateTime(now);

        // Assert
        assertEquals(1L, task.getId());
        assertEquals("TASK-001", task.getTaskNo());
        assertEquals("PAYMENT_CALLBACK", task.getTaskType());
        assertEquals("ORDER", task.getBizType());
        assertEquals("ORD-20260610-001", task.getBizNo());
        assertEquals("MSG-001", task.getMessageId());
        assertEquals("PENDING", task.getTaskStatus());
        assertEquals(0, task.getRetryCount());
        assertEquals(16, task.getMaxRetryCount());
        assertEquals(nextRetry, task.getNextRetryTime());
        assertEquals("connection timeout", task.getLastErrorMessage());
        assertEquals("{\"key\":\"value\"}", task.getExtensionJson());
        assertEquals(now, task.getCreateTime());
        assertEquals(now, task.getUpdateTime());
    }

    @Test
    void defaultFields_shouldBeNull_whenNewInstanceCreated() {
        // Arrange & Act
        RetryTask task = new RetryTask();

        // Assert
        assertNull(task.getId());
        assertNull(task.getTaskNo());
        assertNull(task.getTaskType());
        assertNull(task.getBizType());
        assertNull(task.getBizNo());
        assertNull(task.getMessageId());
        assertNull(task.getTaskStatus());
        assertNull(task.getRetryCount());
        assertNull(task.getMaxRetryCount());
        assertNull(task.getNextRetryTime());
        assertNull(task.getLastErrorMessage());
        assertNull(task.getExtensionJson());
        assertNull(task.getCreateTime());
        assertNull(task.getUpdateTime());
    }

    @Test
    void tableNameAnnotation_shouldBeRetryTask() {
        // Arrange & Act
        TableName annotation = RetryTask.class.getAnnotation(TableName.class);

        // Assert
        assertNotNull(annotation);
        assertEquals("retry_task", annotation.value());
    }

    @Test
    void taskStatus_shouldSupportAllValidValues() {
        // Arrange
        RetryTask task = new RetryTask();
        String[] statuses = {"PENDING", "PROCESSING", "SUCCESS", "FAIL", "DEAD", "CANCELLED"};

        // Act & Assert
        for (String status : statuses) {
            task.setTaskStatus(status);
            assertEquals(status, task.getTaskStatus());
        }
    }

    @Test
    void taskType_shouldSupportAllValidValues() {
        // Arrange
        RetryTask task = new RetryTask();
        String[] types = {
            "PAYMENT_CALLBACK", "ORDER_CLOSE", "RECHARGE_CREDIT",
            "REFUND_QUERY", "COUPON_COMPENSATE", "SMS_RETRY"
        };

        // Act & Assert
        for (String type : types) {
            task.setTaskType(type);
            assertEquals(type, task.getTaskType());
        }
    }

    @Test
    void serializable_shouldBeImplemented() {
        // Arrange & Act
        boolean isSerializable = java.io.Serializable.class.isAssignableFrom(RetryTask.class);

        // Assert
        assertTrue(isSerializable);
    }
}
