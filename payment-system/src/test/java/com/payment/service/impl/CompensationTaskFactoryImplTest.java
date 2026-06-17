package com.payment.service.impl;

import com.payment.entity.CompensationTask;
import com.payment.mapper.CompensationTaskMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompensationTaskFactoryImplTest {

    @Test
    void createIfAbsentShouldReturnExistingTaskWithoutInsert() {
        CompensationTaskMapper mapper = mock(CompensationTaskMapper.class);
        CompensationTask existing = new CompensationTask();
        existing.setId(10L);
        existing.setBizType("LATE_CALLBACK_REVIEW");
        existing.setBizNo("PB100");
        when(mapper.selectOne(any())).thenReturn(existing);

        CompensationTaskFactoryImpl factory = new CompensationTaskFactoryImpl(mapper);

        CompensationTask result = factory.createIfAbsent("LATE_CALLBACK_REVIEW", "PB100", "manual review");

        assertEquals(existing, result);
        verify(mapper, never()).insert(any(CompensationTask.class));
    }

    @Test
    void createIfAbsentShouldInsertPendingTaskWhenMissing() {
        CompensationTaskMapper mapper = mock(CompensationTaskMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        CompensationTaskFactoryImpl factory = new CompensationTaskFactoryImpl(mapper);

        CompensationTask result = factory.createIfAbsent("merchant_approved_refund", "RA001", "refund execution");

        ArgumentCaptor<CompensationTask> captor = ArgumentCaptor.forClass(CompensationTask.class);
        verify(mapper).insert(captor.capture());
        CompensationTask task = captor.getValue();
        assertEquals(task, result);
        assertNotNull(task.getTaskNo());
        assertEquals(true, task.getTaskNo().startsWith("CT"));
        assertEquals("MERCHANT_APPROVED_REFUND", task.getBizType());
        assertEquals("RA001", task.getBizNo());
        assertEquals("PENDING", task.getTaskStatus());
        assertEquals("refund execution", task.getRemark());
        assertEquals(0, task.getRetryCount());
    }
}
