package com.payment.service.impl;

import com.payment.util.JsonUtils;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.entity.PaymentBill;
import com.payment.entity.MessageOutbox;
import com.payment.entity.SalesOrder;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.PayStatusEnum;
import com.payment.enums.PaymentStatusReasonEnum;
import com.payment.mapper.CompensationTaskMapper;
import com.payment.mapper.MessageOutboxMapper;
import com.payment.mapper.PaymentBillMapper;
import com.payment.mapper.PaymentCallbackRecordMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.PaymentProvider;
import com.payment.service.RefundService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentBillV1ServiceImplTest {

    @Test
    void closedSalesOrderLateCallbackShouldPrepareRefundInsteadOfPublishingOrderPaid() {
        PaymentBillMapper paymentBillMapper = mock(PaymentBillMapper.class);
        PaymentCallbackRecordMapper callbackRecordMapper = mock(PaymentCallbackRecordMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        CompensationTaskMapper compensationTaskMapper = mock(CompensationTaskMapper.class);
        MessageOutboxMapper messageOutboxMapper = mock(MessageOutboxMapper.class);
        RefundService refundService = mock(RefundService.class);
        PaymentProvider provider = mock(PaymentProvider.class);

        when(provider.getChannelCode()).thenReturn("ALIPAY_PAGE");
        when(provider.verifyCallback(any())).thenReturn(true);
        when(callbackRecordMapper.selectOne(any())).thenReturn(null);
        when(paymentBillMapper.selectOne(any())).thenReturn(buildClosedBill(
                "PB100",
                "SALES_ORDER",
                PaymentStatusReasonEnum.SALES_ORDER_CANCELLED_REFUND_REQUIRED
        ));
        when(salesOrderMapper.selectByOrderNoForUpdate("BIZ-PB100"))
                .thenReturn(closedOrder("BIZ-PB100"));
        when(paymentBillMapper.markLatePaidIfClosed(any(), any(), any(), any())).thenReturn(1);

        PaymentBillV1ServiceImpl service = new PaymentBillV1ServiceImpl(
                paymentBillMapper,
                callbackRecordMapper,
                salesOrderMapper,
                new CompensationTaskFactoryImpl(compensationTaskMapper),
                new OutboxPublisherImpl(messageOutboxMapper),
                List.of(provider),
                refundService
        );

        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB100");
        dto.setCallbackRequestId("notify-1");
        dto.setThirdPartyBillNo("trade-1");
        dto.setRawBody("{\"trade_status\":\"TRADE_SUCCESS\"}");

        service.handleCallback("ALIPAY_PAGE", dto);

        verify(paymentBillMapper).markLatePaidIfClosed(
                eq("PB100"), any(), eq("trade-1"), any());
        verify(refundService).prepareLateCallbackRefund(any(PaymentBill.class), eq(PaymentStatusReasonEnum.SALES_ORDER_CANCELLED_REFUND_REQUIRED));
        verify(messageOutboxMapper, never()).insert(any(MessageOutbox.class));
    }

    @Test
    void closedRechargeLateCallbackShouldRecoverAndInsertOutboxRecord() {
        PaymentBillMapper paymentBillMapper = mock(PaymentBillMapper.class);
        PaymentCallbackRecordMapper callbackRecordMapper = mock(PaymentCallbackRecordMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        CompensationTaskMapper compensationTaskMapper = mock(CompensationTaskMapper.class);
        MessageOutboxMapper messageOutboxMapper = mock(MessageOutboxMapper.class);
        RefundService refundService = mock(RefundService.class);
        PaymentProvider provider = mock(PaymentProvider.class);

        when(provider.getChannelCode()).thenReturn("ALIPAY_PAGE");
        when(provider.verifyCallback(any())).thenReturn(true);
        when(callbackRecordMapper.selectOne(any())).thenReturn(null);
        when(paymentBillMapper.selectOne(any())).thenReturn(buildClosedBill(
                "PB200",
                "RECHARGE",
                PaymentStatusReasonEnum.RECHARGE_TIMEOUT_RECOVERABLE
        ));
        when(paymentBillMapper.markLatePaidIfClosed(any(), any(), any(), any())).thenReturn(1);
        doAnswer(invocation -> {
            MessageOutbox outbox = invocation.getArgument(0);
            outbox.setId(1L);
            return 1;
        }).when(messageOutboxMapper).insert(any(MessageOutbox.class));

        PaymentBillV1ServiceImpl service = new PaymentBillV1ServiceImpl(
                paymentBillMapper,
                callbackRecordMapper,
                salesOrderMapper,
                new CompensationTaskFactoryImpl(compensationTaskMapper),
                new OutboxPublisherImpl(messageOutboxMapper),
                List.of(provider),
                refundService
        );

        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB200");
        dto.setCallbackRequestId("notify-2");
        dto.setThirdPartyBillNo("trade-2");
        dto.setRawBody("{\"trade_status\":\"TRADE_SUCCESS\"}");

        service.handleCallback("ALIPAY_PAGE", dto);

        verify(refundService, never()).prepareLateCallbackRefund(any(), any());
        verify(messageOutboxMapper).insert(any(MessageOutbox.class));
    }

    private SalesOrder closedOrder(String orderNo) {
        SalesOrder order = new SalesOrder();
        order.setId(10L);
        order.setOrderNo(orderNo);
        order.setOrderStatus(OrderStatusEnum.CLOSED.name());
        order.setPayStatus(PayStatusEnum.CLOSED.name());
        return order;
    }

    private PaymentBill buildClosedBill(String billNo, String bizType, PaymentStatusReasonEnum reasonEnum) {
        PaymentBill bill = new PaymentBill();
        bill.setBillNo(billNo);
        bill.setBizType(bizType);
        bill.setBizNo("BIZ-" + billNo);
        bill.setChannelCode("ALIPAY_PAGE");
        bill.setPayAmount(new BigDecimal("18.80"));
        bill.setPayStatus(PayStatusEnum.CLOSED.name());
        bill.setExpireTime(LocalDateTime.now().minusMinutes(1));
        bill.setExtensionJson(JsonUtils.toJson(Map.of("statusReasonCode", reasonEnum.getCode())));
        return bill;
    }
}

