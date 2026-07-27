package com.payment.service.impl;

import com.payment.config.TestRedissonConfig;
import com.payment.config.TestSaTokenConfig;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.enums.PaymentCallbackFailureReasonEnum;
import com.payment.service.PaymentCallbackAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestSaTokenConfig.class, TestRedissonConfig.class,
        PaymentCallbackAuditTransactionIntegrationTest.TransactionTestConfig.class})
class PaymentCallbackAuditTransactionIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RejectingOuterTransaction rejectingOuterTransaction;

    @BeforeEach
    void clearAuditTable() {
        jdbcTemplate.update("DELETE FROM payment_callback_failure_audit");
    }

    @Test
    void rejectedAuditShouldSurviveOuterTransactionRollback() {
        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB-TX-100");
        dto.setCallbackRequestId("notify-tx-100");
        dto.setRawBody("{\"out_trade_no\":\"PB-TX-100\"}");

        assertThatThrownBy(() -> rejectingOuterTransaction.recordThenRollback(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("force outer rollback");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_callback_failure_audit WHERE candidate_bill_no = ?",
                Integer.class,
                "PB-TX-100");
        assertThat(count).isEqualTo(1);
    }

    @TestConfiguration
    static class TransactionTestConfig {
        @Bean
        RejectingOuterTransaction rejectingOuterTransaction(PaymentCallbackAuditService auditService) {
            return new RejectingOuterTransaction(auditService);
        }
    }

    static class RejectingOuterTransaction {
        private final PaymentCallbackAuditService auditService;

        RejectingOuterTransaction(PaymentCallbackAuditService auditService) {
            this.auditService = auditService;
        }

        @Transactional
        public void recordThenRollback(PaymentCallbackDTO dto) {
            auditService.recordRejected(
                    "ALIPAY_PAGE", dto, PaymentCallbackFailureReasonEnum.SIGNATURE_INVALID);
            throw new IllegalStateException("force outer rollback");
        }
    }
}
