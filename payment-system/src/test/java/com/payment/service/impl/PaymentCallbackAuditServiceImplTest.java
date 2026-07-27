package com.payment.service.impl;

import com.payment.dto.PaymentCallbackDTO;
import com.payment.entity.PaymentCallbackFailureAudit;
import com.payment.enums.MessageProcessStatusEnum;
import com.payment.enums.PaymentCallbackFailureReasonEnum;
import com.payment.mapper.PaymentCallbackFailureAuditMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentCallbackAuditServiceImplTest {

    @Test
    void recordRejectedShouldUseRequiresNewTransaction() throws Exception {
        Method method = PaymentCallbackAuditServiceImpl.class.getMethod(
                "recordRejected", String.class, PaymentCallbackDTO.class, PaymentCallbackFailureReasonEnum.class);

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void recordRejectedShouldPersistFinalFailureState() {
        PaymentCallbackFailureAuditMapper mapper = mock(PaymentCallbackFailureAuditMapper.class);
        PaymentCallbackAuditServiceImpl service = new PaymentCallbackAuditServiceImpl(mapper);
        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB-100");
        dto.setCallbackRequestId("notify-100");
        dto.setRawBody("{\"out_trade_no\":\"PB-100\"}");

        service.recordRejected("ALIPAY_PAGE", dto, PaymentCallbackFailureReasonEnum.SIGNATURE_INVALID);

        ArgumentCaptor<PaymentCallbackFailureAudit> captor = ArgumentCaptor.forClass(PaymentCallbackFailureAudit.class);
        verify(mapper).upsertWindow(captor.capture());
        PaymentCallbackFailureAudit record = captor.getValue();
        assertThat(record.getEventId()).isNotBlank();
        assertThat(record.getCandidateBillNo()).isEqualTo("PB-100");
        assertThat(record.getChannelCode()).isEqualTo("ALIPAY_PAGE");
        assertThat(record.getProviderRequestId()).isEqualTo("notify-100");
        assertThat(record.getPayloadSha256()).hasSize(64);
        assertThat(record.getPayloadSize()).isEqualTo(dto.getRawBody().getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        assertThat(record.getVerifyStatus()).isEqualTo(MessageProcessStatusEnum.FAILED.name());
        assertThat(record.getFailureReason()).isEqualTo(PaymentCallbackFailureReasonEnum.SIGNATURE_INVALID.name());
        assertThat(record.getOccurrenceCount()).isEqualTo(1L);
        assertThat(record.getWindowStart()).isNotNull();
        assertThat(record.getLastTime()).isEqualTo(record.getCreateTime());
        assertThat(record.getCreateTime()).isNotNull();
        assertThat(record.getExpireTime()).isAfter(record.getCreateTime().plusDays(89));
    }

    @Test
    void recordRejectedShouldGenerateStableBoundedIdentifiersAndBody() {
        PaymentCallbackFailureAuditMapper mapper = mock(PaymentCallbackFailureAuditMapper.class);
        PaymentCallbackAuditServiceImpl service = new PaymentCallbackAuditServiceImpl(mapper);
        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setRawBody("x".repeat(20_000));

        service.recordRejected("CHANNEL-" + "x".repeat(40), dto,
                PaymentCallbackFailureReasonEnum.SIGNED_PAYLOAD_MISMATCH);
        service.recordRejected("CHANNEL-" + "x".repeat(40), dto,
                PaymentCallbackFailureReasonEnum.SIGNED_PAYLOAD_MISMATCH);

        ArgumentCaptor<PaymentCallbackFailureAudit> captor = ArgumentCaptor.forClass(PaymentCallbackFailureAudit.class);
        verify(mapper, org.mockito.Mockito.times(2)).upsertWindow(captor.capture());
        PaymentCallbackFailureAudit first = captor.getAllValues().get(0);
        PaymentCallbackFailureAudit second = captor.getAllValues().get(1);
        assertThat(first.getChannelCode()).hasSizeLessThanOrEqualTo(32);
        assertThat(first.getCandidateBillNo()).isNull();
        assertThat(first.getProviderRequestId()).isNull();
        assertThat(first.getPayloadSha256()).isEqualTo(second.getPayloadSha256());
        assertThat(first.getPayloadSize()).isEqualTo(20_000);
        assertThat(first.getEventId()).isNotEqualTo(second.getEventId());
    }

    @Test
    void recordRejectedShouldNotStoreRawPayloadOrUseBusinessCallbackTable() {
        PaymentCallbackFailureAuditMapper mapper = mock(PaymentCallbackFailureAuditMapper.class);
        PaymentCallbackAuditServiceImpl service = new PaymentCallbackAuditServiceImpl(mapper);
        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB-200");
        dto.setCallbackRequestId("notify-200");
        dto.setRawBody("tampered");

        service.recordRejected("ALIPAY_PAGE", dto, PaymentCallbackFailureReasonEnum.SIGNATURE_INVALID);

        ArgumentCaptor<PaymentCallbackFailureAudit> captor = ArgumentCaptor.forClass(PaymentCallbackFailureAudit.class);
        verify(mapper).upsertWindow(captor.capture());
        assertThat(captor.getValue().getPayloadSha256()).isNotEqualTo(dto.getRawBody());
    }
}
