package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.entity.PaymentBill;
import com.payment.entity.PaymentCallbackRecord;
import com.payment.enums.PaymentCallbackFailureReasonEnum;
import com.payment.mapper.CompensationTaskMapper;
import com.payment.mapper.MessageOutboxMapper;
import com.payment.mapper.PaymentBillMapper;
import com.payment.mapper.PaymentCallbackRecordMapper;
import com.payment.mapper.RechargeOrderV1Mapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.OrderPaymentFailureService;
import com.payment.service.PaymentCallbackAuditService;
import com.payment.service.PaymentProvider;
import com.payment.service.RefundService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentBillCallbackAuditTest {

    @Test
    void invalidSignatureShouldWriteIndependentFailureAuditBeforeThrowing() {
        Fixture fixture = new Fixture();
        when(fixture.provider.verifyCallback(any())).thenReturn(false);
        PaymentCallbackDTO dto = fixture.callback("PB-300", "notify-invalid",
                "{\"out_trade_no\":\"PB-300\",\"trade_status\":\"TRADE_SUCCESS\"}");

        assertThatThrownBy(() -> fixture.service.handleCallback("ALIPAY_PAGE", dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("支付回调验签失败");

        verify(fixture.callbackAuditService).recordRejected(
                "ALIPAY_PAGE", dto, PaymentCallbackFailureReasonEnum.SIGNATURE_INVALID);
        verify(fixture.callbackRecordMapper, never()).insert(any(PaymentCallbackRecord.class));
        verify(fixture.paymentBillMapper, never()).markPaidIfPending(any(), any(), any(), any(), any());
    }

    @Test
    void signedPayloadMismatchShouldWriteIndependentFailureAuditBeforeThrowing() {
        Fixture fixture = new Fixture();
        when(fixture.provider.verifyCallback(any())).thenReturn(true);
        PaymentCallbackDTO dto = fixture.callback("PB-300", "notify-mismatch",
                "{\"out_trade_no\":\"PB-999\",\"trade_status\":\"TRADE_SUCCESS\"}");

        assertThatThrownBy(() -> fixture.service.handleCallback("ALIPAY_PAGE", dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("支付账单号与已验签报文不一致");

        verify(fixture.callbackAuditService).recordRejected(
                "ALIPAY_PAGE", dto, PaymentCallbackFailureReasonEnum.SIGNED_PAYLOAD_MISMATCH);
        verify(fixture.callbackRecordMapper, never()).insert(any(PaymentCallbackRecord.class));
        verify(fixture.paymentBillMapper, never()).markPaidIfPending(any(), any(), any(), any(), any());
    }

    @Test
    void auditStorageFailureShouldNotMaskSignatureRejection() {
        Fixture fixture = new Fixture();
        when(fixture.provider.verifyCallback(any())).thenReturn(false);
        org.mockito.Mockito.doThrow(new IllegalStateException("database unavailable"))
                .when(fixture.callbackAuditService).recordRejected(
                        any(), any(), eq(PaymentCallbackFailureReasonEnum.SIGNATURE_INVALID));
        PaymentCallbackDTO dto = fixture.callback("PB-300", "notify-audit-down", "{}");

        assertThatThrownBy(() -> fixture.service.handleCallback("ALIPAY_PAGE", dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("支付回调验签失败");
    }

    @Test
    void oversizedPayloadShouldBeRejectedBeforeProviderVerification() {
        Fixture fixture = new Fixture();
        PaymentCallbackDTO dto = fixture.callback(
                "PB-300", "notify-oversized", "x".repeat(70_000));

        assertThatThrownBy(() -> fixture.service.handleCallback("ALIPAY_PAGE", dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("支付回调报文过大");

        verify(fixture.callbackAuditService).recordRejected(
                "ALIPAY_PAGE", dto, PaymentCallbackFailureReasonEnum.PAYLOAD_INVALID);
        verify(fixture.provider, never()).verifyCallback(any());
    }

    @Test
    void callbackIdOwnershipConflictShouldBeAuditedAndRejected() {
        Fixture fixture = new Fixture();
        when(fixture.provider.verifyCallback(any())).thenReturn(true);
        PaymentCallbackRecord existing = new PaymentCallbackRecord();
        existing.setId(99L);
        existing.setBillNo("PB-OTHER");
        existing.setProcessStatus("SUCCESS");
        when(fixture.callbackRecordMapper.selectOne(any())).thenReturn(existing);
        PaymentCallbackDTO dto = fixture.callback("PB-300", null,
                "{\"out_trade_no\":\"PB-300\",\"trade_status\":\"TRADE_SUCCESS\"}");

        assertThatThrownBy(() -> fixture.service.handleCallback("ALIPAY_PAGE", dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("支付回调幂等键归属冲突");

        verify(fixture.callbackAuditService).recordRejected(
                "ALIPAY_PAGE", dto, PaymentCallbackFailureReasonEnum.IDEMPOTENCY_CONFLICT);
    }

    @Test
    void legacyProviderRequestIdShouldStillDeduplicateHistoricalSuccess() {
        Fixture fixture = new Fixture();
        when(fixture.provider.verifyCallback(any())).thenReturn(true);
        PaymentCallbackRecord legacy = new PaymentCallbackRecord();
        legacy.setId(100L);
        legacy.setBillNo("PB-300");
        legacy.setChannelCode("ALIPAY_PAGE");
        legacy.setCallbackRequestId("notify-legacy");
        legacy.setProcessStatus("SUCCESS");
        when(fixture.callbackRecordMapper.selectOne(any())).thenReturn(null, legacy);
        PaymentCallbackDTO dto = fixture.callback("PB-300", "notify-legacy",
                "{\"out_trade_no\":\"PB-300\",\"notify_id\":\"notify-legacy\","
                        + "\"trade_status\":\"TRADE_SUCCESS\"}");

        fixture.service.handleCallback("ALIPAY_PAGE", dto);

        verify(fixture.callbackRecordMapper, times(2)).selectOne(any());
        verify(fixture.callbackRecordMapper, never()).insert(any(PaymentCallbackRecord.class));
    }

    private static class Fixture {
        private final PaymentBillMapper paymentBillMapper = mock(PaymentBillMapper.class);
        private final PaymentCallbackRecordMapper callbackRecordMapper = mock(PaymentCallbackRecordMapper.class);
        private final SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        private final PaymentCallbackAuditService callbackAuditService = mock(PaymentCallbackAuditService.class);
        private final PaymentProvider provider = mock(PaymentProvider.class);
        private final PaymentBillV1ServiceImpl service;

        private Fixture() {
            PaymentBill bill = new PaymentBill();
            bill.setBillNo("PB-300");
            bill.setBizType("SALES_ORDER");
            bill.setBizNo("SO-300");
            bill.setChannelCode("ALIPAY_PAGE");
            bill.setPayStatus("WAIT_PAY");
            when(paymentBillMapper.selectOne(any())).thenReturn(bill);
            when(callbackRecordMapper.selectOne(any())).thenReturn(null);
            when(provider.getChannelCode()).thenReturn("ALIPAY_PAGE");
            service = new PaymentBillV1ServiceImpl(
                    paymentBillMapper,
                    callbackRecordMapper,
                    salesOrderMapper,
                    new CompensationTaskFactoryImpl(mock(CompensationTaskMapper.class)),
                    new OutboxPublisherImpl(mock(MessageOutboxMapper.class)),
                    List.of(provider),
                    mock(RefundService.class),
                    mock(OrderPaymentFailureService.class),
                    mock(RechargeOrderV1Mapper.class),
                    callbackAuditService
            );
        }

        private PaymentCallbackDTO callback(String billNo, String requestId, String rawBody) {
            PaymentCallbackDTO dto = new PaymentCallbackDTO();
            dto.setBillNo(billNo);
            dto.setCallbackRequestId(requestId);
            dto.setRawBody(rawBody);
            return dto;
        }
    }
}
