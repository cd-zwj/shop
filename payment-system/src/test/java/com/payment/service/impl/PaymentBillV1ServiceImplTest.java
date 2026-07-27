package com.payment.service.impl;

import com.payment.util.JsonUtils;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.entity.PaymentBill;
import com.payment.entity.RechargeOrderV1;
import com.payment.entity.MessageOutbox;
import com.payment.entity.SalesOrder;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.PayStatusEnum;
import com.payment.enums.PaymentStatusReasonEnum;
import com.payment.mapper.CompensationTaskMapper;
import com.payment.mapper.MessageOutboxMapper;
import com.payment.mapper.PaymentBillMapper;
import com.payment.mapper.PaymentCallbackRecordMapper;
import com.payment.mapper.RechargeOrderV1Mapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.PaymentProvider;
import com.payment.service.RefundService;
import com.payment.service.OrderPaymentFailureService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
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
        OrderPaymentFailureService orderPaymentFailureService = mock(OrderPaymentFailureService.class);
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
                refundService,
                orderPaymentFailureService,
                mock(RechargeOrderV1Mapper.class)
        );

        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB100");
        dto.setCallbackRequestId("notify-1");
        dto.setThirdPartyBillNo("trade-1");
        dto.setRawBody("{\"out_trade_no\":\"PB100\",\"trade_no\":\"trade-1\",\"trade_status\":\"TRADE_SUCCESS\"}");
        dto.setSuccess(true);

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
        OrderPaymentFailureService orderPaymentFailureService = mock(OrderPaymentFailureService.class);
        RechargeOrderV1Mapper rechargeOrderV1Mapper = mock(RechargeOrderV1Mapper.class);
        PaymentProvider provider = mock(PaymentProvider.class);

        when(provider.getChannelCode()).thenReturn("ALIPAY_PAGE");
        when(provider.verifyCallback(any())).thenReturn(true);
        when(callbackRecordMapper.selectOne(any())).thenReturn(null);
        when(paymentBillMapper.selectOne(any())).thenReturn(buildClosedBill(
                "PB200",
                "RECHARGE",
                PaymentStatusReasonEnum.RECHARGE_TIMEOUT_RECOVERABLE
        ));
        when(rechargeOrderV1Mapper.selectByRechargeNoForUpdate("BIZ-PB200"))
                .thenReturn(rechargeOrder("BIZ-PB200", PayStatusEnum.WAIT_PAY.name()));
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
                refundService,
                orderPaymentFailureService,
                rechargeOrderV1Mapper
        );

        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB200");
        dto.setCallbackRequestId("notify-2");
        dto.setThirdPartyBillNo("trade-2");
        dto.setRawBody("{\"out_trade_no\":\"PB200\",\"trade_no\":\"trade-2\",\"trade_status\":\"TRADE_SUCCESS\"}");
        dto.setSuccess(true);

        service.handleCallback("ALIPAY_PAGE", dto);

        verify(refundService, never()).prepareLateCallbackRefund(any(), any());
        verify(messageOutboxMapper).insert(any(MessageOutbox.class));
    }

    @Test
    void terminalRechargeFailureShouldUpdateRechargeOrderAndPaymentBill() {
        PaymentBillMapper paymentBillMapper = mock(PaymentBillMapper.class);
        PaymentCallbackRecordMapper callbackRecordMapper = mock(PaymentCallbackRecordMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        CompensationTaskMapper compensationTaskMapper = mock(CompensationTaskMapper.class);
        MessageOutboxMapper messageOutboxMapper = mock(MessageOutboxMapper.class);
        RefundService refundService = mock(RefundService.class);
        OrderPaymentFailureService orderPaymentFailureService = mock(OrderPaymentFailureService.class);
        RechargeOrderV1Mapper rechargeOrderV1Mapper = mock(RechargeOrderV1Mapper.class);
        PaymentProvider provider = mock(PaymentProvider.class);

        PaymentBill pendingBill = new PaymentBill();
        pendingBill.setBillNo("PB225");
        pendingBill.setBizType("RECHARGE");
        pendingBill.setBizNo("RC225");
        pendingBill.setChannelCode("ALIPAY_PAGE");
        pendingBill.setPayStatus(PayStatusEnum.WAIT_PAY.name());

        when(provider.getChannelCode()).thenReturn("ALIPAY_PAGE");
        when(provider.verifyCallback(any())).thenReturn(true);
        when(callbackRecordMapper.selectOne(any())).thenReturn(null);
        when(paymentBillMapper.selectOne(any())).thenReturn(pendingBill);
        when(rechargeOrderV1Mapper.selectByRechargeNoForUpdate("RC225"))
                .thenReturn(rechargeOrder("RC225", PayStatusEnum.WAIT_PAY.name()));
        when(rechargeOrderV1Mapper.failIfPending("RC225")).thenReturn(1);
        when(paymentBillMapper.markFailedIfPending(eq("PB225"), eq("CALLBACK_SUCCESS"), any(), any()))
                .thenReturn(1);

        PaymentBillV1ServiceImpl service = new PaymentBillV1ServiceImpl(
                paymentBillMapper, callbackRecordMapper, salesOrderMapper,
                new CompensationTaskFactoryImpl(compensationTaskMapper),
                new OutboxPublisherImpl(messageOutboxMapper), List.of(provider), refundService,
                orderPaymentFailureService, rechargeOrderV1Mapper);

        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB225");
        dto.setCallbackRequestId("notify-recharge-failed");
        dto.setSuccess(false);
        dto.setTerminalFailure(true);
        dto.setRawBody("{\"out_trade_no\":\"PB225\",\"trade_status\":\"TRADE_CLOSED\"}");

        service.handleCallback("ALIPAY_PAGE", dto);

        verify(rechargeOrderV1Mapper).failIfPending("RC225");
        verify(paymentBillMapper).markFailedIfPending(eq("PB225"), eq("CALLBACK_SUCCESS"), any(), any());
        verify(messageOutboxMapper, never()).insert(any(MessageOutbox.class));
    }

    @Test
    void failedSalesOrderLateSuccessShouldPrepareRefundWithoutPublishingOrderPaid() {
        PaymentBillMapper paymentBillMapper = mock(PaymentBillMapper.class);
        PaymentCallbackRecordMapper callbackRecordMapper = mock(PaymentCallbackRecordMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        CompensationTaskMapper compensationTaskMapper = mock(CompensationTaskMapper.class);
        MessageOutboxMapper messageOutboxMapper = mock(MessageOutboxMapper.class);
        RefundService refundService = mock(RefundService.class);
        OrderPaymentFailureService orderPaymentFailureService = mock(OrderPaymentFailureService.class);
        PaymentProvider provider = mock(PaymentProvider.class);

        PaymentBill failedBill = buildClosedBill(
                "PB250", "SALES_ORDER", PaymentStatusReasonEnum.SALES_ORDER_PAYMENT_FAILED_REFUND_REQUIRED);
        failedBill.setPayStatus(PayStatusEnum.FAILED.name());
        SalesOrder failedOrder = closedOrder("BIZ-PB250");
        failedOrder.setPayStatus(PayStatusEnum.FAILED.name());

        when(provider.getChannelCode()).thenReturn("ALIPAY_PAGE");
        when(provider.verifyCallback(any())).thenReturn(true);
        when(callbackRecordMapper.selectOne(any())).thenReturn(null);
        when(paymentBillMapper.selectOne(any())).thenReturn(failedBill);
        when(salesOrderMapper.selectByOrderNoForUpdate("BIZ-PB250")).thenReturn(failedOrder);
        when(paymentBillMapper.markLatePaidIfClosed(any(), any(), any(), any())).thenReturn(1);

        PaymentBillV1ServiceImpl service = new PaymentBillV1ServiceImpl(
                paymentBillMapper, callbackRecordMapper, salesOrderMapper,
                new CompensationTaskFactoryImpl(compensationTaskMapper),
                new OutboxPublisherImpl(messageOutboxMapper), List.of(provider), refundService,
                orderPaymentFailureService, mock(RechargeOrderV1Mapper.class));

        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB250");
        dto.setCallbackRequestId("notify-late-success");
        dto.setThirdPartyBillNo("trade-250");
        dto.setSuccess(true);
        dto.setRawBody("{\"out_trade_no\":\"PB250\",\"trade_no\":\"trade-250\",\"trade_status\":\"TRADE_SUCCESS\"}");

        service.handleCallback("ALIPAY_PAGE", dto);

        verify(paymentBillMapper).markLatePaidIfClosed(eq("PB250"), any(), eq("trade-250"), any());
        verify(refundService).prepareLateCallbackRefund(
                any(PaymentBill.class), eq(PaymentStatusReasonEnum.SALES_ORDER_PAYMENT_FAILED_REFUND_REQUIRED));
        verify(messageOutboxMapper, never()).insert(any(MessageOutbox.class));
        verify(orderPaymentFailureService, never()).failAndRelease(any(), any());
    }

    @Test
    void failedSalesOrderCallbackShouldReleaseReservationsWithoutPublishingOrderPaid() {
        PaymentBillMapper paymentBillMapper = mock(PaymentBillMapper.class);
        PaymentCallbackRecordMapper callbackRecordMapper = mock(PaymentCallbackRecordMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        CompensationTaskMapper compensationTaskMapper = mock(CompensationTaskMapper.class);
        MessageOutboxMapper messageOutboxMapper = mock(MessageOutboxMapper.class);
        RefundService refundService = mock(RefundService.class);
        OrderPaymentFailureService orderPaymentFailureService = mock(OrderPaymentFailureService.class);
        PaymentProvider provider = mock(PaymentProvider.class);

        PaymentBill pendingBill = new PaymentBill();
        pendingBill.setBillNo("PB300");
        pendingBill.setBizType("SALES_ORDER");
        pendingBill.setBizNo("SO300");
        pendingBill.setChannelCode("ALIPAY_PAGE");
        pendingBill.setPayStatus(PayStatusEnum.WAIT_PAY.name());
        pendingBill.setTenantId(9L);
        pendingBill.setPlatformUserId(100L);

        when(provider.getChannelCode()).thenReturn("ALIPAY_PAGE");
        when(provider.verifyCallback(any())).thenReturn(true);
        when(callbackRecordMapper.selectOne(any())).thenReturn(null);
        when(paymentBillMapper.selectOne(any())).thenReturn(pendingBill);
        when(paymentBillMapper.markFailedIfPending(eq("PB300"), eq("CALLBACK_SUCCESS"),
                eq("支付渠道返回失败"), any()))
                .thenReturn(1);
        SalesOrder pendingOrder = new SalesOrder();
        pendingOrder.setOrderStatus(OrderStatusEnum.CREATED.name());
        pendingOrder.setPayStatus(PayStatusEnum.WAIT_PAY.name());
        pendingOrder.setTenantId(9L);
        pendingOrder.setPlatformUserId(100L);
        when(salesOrderMapper.selectByOrderNoForUpdate("SO300")).thenReturn(pendingOrder);
        when(orderPaymentFailureService.failAndRelease("SO300", "支付渠道返回失败")).thenReturn(true);

        PaymentBillV1ServiceImpl service = new PaymentBillV1ServiceImpl(
                paymentBillMapper,
                callbackRecordMapper,
                salesOrderMapper,
                new CompensationTaskFactoryImpl(compensationTaskMapper),
                new OutboxPublisherImpl(messageOutboxMapper),
                List.of(provider),
                refundService,
                orderPaymentFailureService,
                mock(RechargeOrderV1Mapper.class)
        );

        PaymentCallbackDTO pendingDto = new PaymentCallbackDTO();
        pendingDto.setBillNo("PB300");
        pendingDto.setCallbackRequestId("notify-pending-1");
        pendingDto.setSuccess(false);
        pendingDto.setTerminalFailure(false);
        pendingDto.setRawBody("{\"out_trade_no\":\"PB300\",\"trade_status\":\"WAIT_BUYER_PAY\"}");

        service.handleCallback("ALIPAY_PAGE", pendingDto);

        verify(paymentBillMapper, never()).markFailedIfPending(any(), any(), any(), any());
        verify(orderPaymentFailureService, never()).failAndRelease(any(), any());

        PaymentCallbackDTO forgedSuccessDto = new PaymentCallbackDTO();
        forgedSuccessDto.setBillNo("PB300");
        forgedSuccessDto.setCallbackRequestId("notify-forged-success");
        forgedSuccessDto.setSuccess(true);
        forgedSuccessDto.setTerminalFailure(false);
        forgedSuccessDto.setRawBody("{\"out_trade_no\":\"PB300\",\"trade_status\":\"TRADE_CLOSED\"}");

        assertThrows(com.payment.common.BusinessException.class,
                () -> service.handleCallback("ALIPAY_PAGE", forgedSuccessDto));
        verify(salesOrderMapper, never()).claimPayment(any());

        PaymentCallbackDTO mismatchedDto = new PaymentCallbackDTO();
        mismatchedDto.setBillNo("PB300");
        mismatchedDto.setCallbackRequestId("notify-mismatch-1");
        mismatchedDto.setSuccess(false);
        mismatchedDto.setTerminalFailure(true);
        mismatchedDto.setRawBody("{\"out_trade_no\":\"PB999\",\"trade_status\":\"TRADE_CLOSED\"}");

        assertThrows(com.payment.common.BusinessException.class,
                () -> service.handleCallback("ALIPAY_PAGE", mismatchedDto));
        verify(paymentBillMapper, never()).markFailedIfPending(any(), any(), any(), any());
        verify(orderPaymentFailureService, never()).failAndRelease(any(), any());

        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB300");
        dto.setCallbackRequestId("notify-failed-1");
        dto.setSuccess(false);
        dto.setTerminalFailure(true);
        dto.setRawBody("{\"out_trade_no\":\"PB300\",\"trade_status\":\"TRADE_CLOSED\"}");

        service.handleCallback("ALIPAY_PAGE", dto);

        InOrder failureOrder = inOrder(salesOrderMapper, orderPaymentFailureService, paymentBillMapper);
        failureOrder.verify(salesOrderMapper).selectByOrderNoForUpdate("SO300");
        failureOrder.verify(orderPaymentFailureService).failAndRelease("SO300", "支付渠道返回失败");
        failureOrder.verify(paymentBillMapper).markFailedIfPending(eq("PB300"), eq("CALLBACK_SUCCESS"),
                eq("支付渠道返回失败"), any());
        verify(paymentBillMapper).markFailedIfPending(eq("PB300"), eq("CALLBACK_SUCCESS"),
                eq("支付渠道返回失败"), any());
        verify(orderPaymentFailureService).failAndRelease("SO300", "支付渠道返回失败");
        verify(salesOrderMapper, never()).claimPayment(any());
        verify(messageOutboxMapper, never()).insert(any(MessageOutbox.class));
        verify(refundService, never()).prepareLateCallbackRefund(any(), any());
    }

    private SalesOrder closedOrder(String orderNo) {
        SalesOrder order = new SalesOrder();
        order.setId(10L);
        order.setOrderNo(orderNo);
        order.setOrderStatus(OrderStatusEnum.CLOSED.name());
        order.setPayStatus(PayStatusEnum.CLOSED.name());
        return order;
    }

    private RechargeOrderV1 rechargeOrder(String rechargeNo, String status) {
        RechargeOrderV1 order = new RechargeOrderV1();
        order.setRechargeNo(rechargeNo);
        order.setBizStatus(status);
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

