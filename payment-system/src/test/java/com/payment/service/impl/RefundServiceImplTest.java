package com.payment.service.impl;

import com.payment.entity.CompensationTask;
import com.payment.entity.PaymentBill;
import com.payment.entity.RefundOrder;
import com.payment.entity.RefundRecord;
import com.payment.mapper.CompensationTaskMapper;
import com.payment.mapper.PaymentBillMapper;
import com.payment.mapper.RefundOrderMapper;
import com.payment.mapper.RefundRecordMapper;
import com.payment.mapper.RefundReconcileTaskMapper;
import com.payment.service.PaymentProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefundServiceImplTest {

    @Test
    void unsupportedProviderShouldFailLateCallbackRefundTask() {
        RefundOrderMapper refundOrderMapper = mock(RefundOrderMapper.class);
        RefundRecordMapper refundRecordMapper = mock(RefundRecordMapper.class);
        RefundReconcileTaskMapper refundReconcileTaskMapper = mock(RefundReconcileTaskMapper.class);
        CompensationTaskMapper compensationTaskMapper = mock(CompensationTaskMapper.class);
        PaymentBillMapper paymentBillMapper = mock(PaymentBillMapper.class);
        PaymentProvider provider = mock(PaymentProvider.class);

        when(provider.getChannelCode()).thenReturn("EXT_PROVIDER");
        when(provider.supportsRefund()).thenReturn(false);

        RefundOrder refundOrder = new RefundOrder();
        refundOrder.setRefundNo("RF001");
        refundOrder.setPaymentBillNo("PB001");
        refundOrder.setChannelCode("EXT_PROVIDER");
        refundOrder.setExternalRefundAmount(new BigDecimal("9.90"));
        refundOrder.setRefundStatus("APPLIED");
        when(refundOrderMapper.selectOne(any())).thenReturn(refundOrder);

        RefundRecord refundRecord = new RefundRecord();
        refundRecord.setRefundNo("RF001");
        refundRecord.setChannelCode("EXT_PROVIDER");
        when(refundRecordMapper.selectOne(any())).thenReturn(refundRecord);

        PaymentBill paymentBill = new PaymentBill();
        paymentBill.setBillNo("PB001");
        paymentBill.setChannelCode("EXT_PROVIDER");
        paymentBill.setPayAmount(new BigDecimal("9.90"));
        when(paymentBillMapper.selectOne(any())).thenReturn(paymentBill);

        RefundServiceImpl service = new RefundServiceImpl(
                refundOrderMapper,
                refundRecordMapper,
                refundReconcileTaskMapper,
                compensationTaskMapper,
                paymentBillMapper,
                List.of(provider)
        );

        CompensationTask task = new CompensationTask();
        task.setTaskNo("CT001");
        task.setBizNo("PB001");
        task.setTaskStatus("PENDING");
        task.setRetryCount(0);

        service.processLateCallbackRefundTask(task);

        assertEquals("FAIL", task.getTaskStatus());
        assertEquals("FAIL", refundOrder.getRefundStatus());
        verify(compensationTaskMapper).updateById(task);
        verify(refundOrderMapper).updateById(refundOrder);
        verify(refundRecordMapper).updateById(refundRecord);
    }
}
