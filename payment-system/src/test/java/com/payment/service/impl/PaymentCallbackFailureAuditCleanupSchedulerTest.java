package com.payment.service.impl;

import com.payment.mapper.PaymentCallbackFailureAuditMapper;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentCallbackFailureAuditCleanupSchedulerTest {

    @Test
    void cleanupShouldDeleteExpiredRowsInBoundedBatches() {
        PaymentCallbackFailureAuditMapper mapper = mock(PaymentCallbackFailureAuditMapper.class);
        when(mapper.deleteExpiredBatch(1_000)).thenReturn(1_000, 10);
        PaymentCallbackFailureAuditCleanupScheduler scheduler =
                new PaymentCallbackFailureAuditCleanupScheduler(mapper);

        scheduler.cleanupExpiredAudits();

        verify(mapper, times(2)).deleteExpiredBatch(1_000);
    }
}
