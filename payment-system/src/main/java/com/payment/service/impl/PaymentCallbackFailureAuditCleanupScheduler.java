package com.payment.service.impl;

import com.payment.mapper.PaymentCallbackFailureAuditMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCallbackFailureAuditCleanupScheduler {

    private static final int BATCH_SIZE = 1_000;
    private static final int MAX_BATCHES_PER_RUN = 100;

    private final PaymentCallbackFailureAuditMapper auditMapper;

    @Scheduled(cron = "${payment.callback-failure-audit.cleanup-cron:0 15 * * * *}")
    public void cleanupExpiredAudits() {
        int totalDeleted = 0;
        for (int batch = 0; batch < MAX_BATCHES_PER_RUN; batch++) {
            int deleted = auditMapper.deleteExpiredBatch(BATCH_SIZE);
            totalDeleted += deleted;
            if (deleted < BATCH_SIZE) {
                break;
            }
        }
        if (totalDeleted > 0) {
            log.info("已清理过期支付回调失败审计, count={}", totalDeleted);
        }
    }
}
