package com.payment.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.payment.entity.CompensationTask;
import com.payment.entity.PaymentBill;
import com.payment.entity.RefundApplication;
import com.payment.entity.RefundOrder;
import com.payment.entity.RefundRecord;
import com.payment.enums.PayStatusEnum;
import com.payment.enums.RefundApplicationStatus;
import com.payment.enums.RefundChannelStatusEnum;
import com.payment.dto.RefundSubmitResultDTO;
import com.payment.mapper.CompensationTaskMapper;
import com.payment.mapper.PaymentBillMapper;
import com.payment.mapper.RefundApplicationMapper;
import com.payment.mapper.RefundOrderMapper;
import com.payment.mapper.RefundRecordMapper;
import com.payment.mapper.RefundReconcileTaskMapper;
import com.payment.service.PaymentProvider;
import com.payment.service.RefundApplicationService;
import com.payment.service.UserNotificationService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefundServiceImplTest {

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CompensationTask.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RefundApplication.class);
    }

    @Test
    void unsupportedProviderShouldFailLateCallbackRefundTask() {
        RefundOrderMapper refundOrderMapper = mock(RefundOrderMapper.class);
        RefundRecordMapper refundRecordMapper = mock(RefundRecordMapper.class);
        RefundReconcileTaskMapper refundReconcileTaskMapper = mock(RefundReconcileTaskMapper.class);
        CompensationTaskMapper compensationTaskMapper = mock(CompensationTaskMapper.class);
        PaymentBillMapper paymentBillMapper = mock(PaymentBillMapper.class);
        RefundApplicationMapper refundApplicationMapper = mock(RefundApplicationMapper.class);
        PaymentProvider provider = mock(PaymentProvider.class);

        when(provider.getChannelCode()).thenReturn("EXT_PROVIDER");
        when(provider.supportsRefund()).thenReturn(false);
        when(compensationTaskMapper.update(isNull(), any())).thenReturn(1);

        RefundOrder refundOrder = refundOrder("RF001", "PB001");
        when(refundOrderMapper.selectOne(any())).thenReturn(refundOrder);

        RefundRecord refundRecord = refundRecord("RF001");
        when(refundRecordMapper.selectOne(any())).thenReturn(refundRecord);

        PaymentBill paymentBill = paymentBill("PB001", "SO1001", new BigDecimal("9.90"));
        when(paymentBillMapper.selectOne(any())).thenReturn(paymentBill);

        RefundServiceImpl service = service(refundOrderMapper, refundRecordMapper, refundReconcileTaskMapper,
                compensationTaskMapper, paymentBillMapper, refundApplicationMapper, List.of(provider));

        CompensationTask task = task("LATE_CALLBACK_REFUND", "PB001");
        service.processLateCallbackRefundTask(task);

        assertEquals("FAIL", task.getTaskStatus());
        assertEquals("FAIL", refundOrder.getRefundStatus());
        assertEquals(RefundChannelStatusEnum.FAIL.name(), refundRecord.getChannelStatus());
        verify(compensationTaskMapper).updateById(task);
        verify(refundOrderMapper).updateById(refundOrder);
        verify(refundRecordMapper).updateById(refundRecord);
        verify(refundApplicationMapper).update(isNull(), any());
    }

    @Test
    void prepareMerchantApprovedRefundShouldCreateRefundOrderRecordAndTask() {
        RefundOrderMapper refundOrderMapper = mock(RefundOrderMapper.class);
        RefundRecordMapper refundRecordMapper = mock(RefundRecordMapper.class);
        RefundReconcileTaskMapper refundReconcileTaskMapper = mock(RefundReconcileTaskMapper.class);
        CompensationTaskMapper compensationTaskMapper = mock(CompensationTaskMapper.class);
        PaymentBillMapper paymentBillMapper = mock(PaymentBillMapper.class);
        RefundApplicationMapper refundApplicationMapper = mock(RefundApplicationMapper.class);

        when(paymentBillMapper.selectOne(any())).thenReturn(paymentBill("PB001", "SO1001", new BigDecimal("100.00")));
        when(refundOrderMapper.selectOne(any())).thenReturn(null);
        when(refundRecordMapper.selectOne(any())).thenReturn(null);
        when(compensationTaskMapper.selectOne(any())).thenReturn(null);

        RefundServiceImpl service = service(refundOrderMapper, refundRecordMapper, refundReconcileTaskMapper,
                compensationTaskMapper, paymentBillMapper, refundApplicationMapper, List.of());

        RefundApplication application = refundApplication();
        service.prepareMerchantApprovedRefund(application);

        ArgumentCaptor<RefundOrder> orderCaptor = ArgumentCaptor.forClass(RefundOrder.class);
        ArgumentCaptor<RefundRecord> recordCaptor = ArgumentCaptor.forClass(RefundRecord.class);
        ArgumentCaptor<CompensationTask> taskCaptor = ArgumentCaptor.forClass(CompensationTask.class);
        verify(refundOrderMapper).insert(orderCaptor.capture());
        verify(refundRecordMapper).insert(recordCaptor.capture());
        verify(compensationTaskMapper).insert(taskCaptor.capture());

        assertEquals(application.getRefundNo(), orderCaptor.getValue().getRefundNo());
        assertEquals("MERCHANT_APPROVED_REFUND", orderCaptor.getValue().getBizType());
        assertEquals(application.getRefundAmount(), orderCaptor.getValue().getRefundAmount());
        assertEquals(application.getRefundNo(), recordCaptor.getValue().getRefundNo());
        assertEquals(application.getRefundAmount(), recordCaptor.getValue().getRefundAmount());
        assertEquals("MERCHANT_APPROVED_REFUND", taskCaptor.getValue().getBizType());
        assertEquals(application.getRefundNo(), taskCaptor.getValue().getBizNo());
    }

    @Test
    void unsupportedProviderShouldMarkMerchantRefundApplicationFailed() {
        RefundOrderMapper refundOrderMapper = mock(RefundOrderMapper.class);
        RefundRecordMapper refundRecordMapper = mock(RefundRecordMapper.class);
        RefundReconcileTaskMapper refundReconcileTaskMapper = mock(RefundReconcileTaskMapper.class);
        CompensationTaskMapper compensationTaskMapper = mock(CompensationTaskMapper.class);
        PaymentBillMapper paymentBillMapper = mock(PaymentBillMapper.class);
        RefundApplicationMapper refundApplicationMapper = mock(RefundApplicationMapper.class);
        PaymentProvider provider = mock(PaymentProvider.class);

        when(provider.getChannelCode()).thenReturn("EXT_PROVIDER");
        when(provider.supportsRefund()).thenReturn(false);
        when(compensationTaskMapper.update(isNull(), any())).thenReturn(1);

        RefundOrder refundOrder = refundOrder("RA001", "PB001");
        when(refundOrderMapper.selectOne(any())).thenReturn(refundOrder);

        RefundRecord refundRecord = refundRecord("RA001");
        when(refundRecordMapper.selectOne(any())).thenReturn(refundRecord);

        PaymentBill paymentBill = paymentBill("PB001", "SO1001", new BigDecimal("9.90"));
        when(paymentBillMapper.selectOne(any())).thenReturn(paymentBill);

        RefundServiceImpl service = service(refundOrderMapper, refundRecordMapper, refundReconcileTaskMapper,
                compensationTaskMapper, paymentBillMapper, refundApplicationMapper, List.of(provider));

        CompensationTask task = task("MERCHANT_APPROVED_REFUND", "RA001");
        service.processLateCallbackRefundTask(task);

        assertEquals("FAIL", refundOrder.getRefundStatus());
        assertEquals(RefundChannelStatusEnum.FAIL.name(), refundRecord.getChannelStatus());
        assertEquals("FAIL", task.getTaskStatus());
        verify(refundApplicationMapper).update(isNull(), any());
    }

    @Test
    void successfulMerchantRefundShouldCompleteRefundApplication() {
        RefundOrderMapper refundOrderMapper = mock(RefundOrderMapper.class);
        RefundRecordMapper refundRecordMapper = mock(RefundRecordMapper.class);
        RefundReconcileTaskMapper refundReconcileTaskMapper = mock(RefundReconcileTaskMapper.class);
        CompensationTaskMapper compensationTaskMapper = mock(CompensationTaskMapper.class);
        PaymentBillMapper paymentBillMapper = mock(PaymentBillMapper.class);
        RefundApplicationMapper refundApplicationMapper = mock(RefundApplicationMapper.class);
        PaymentProvider provider = mock(PaymentProvider.class);
        RefundApplicationService refundApplicationService = mock(RefundApplicationService.class);

        when(provider.getChannelCode()).thenReturn("EXT_PROVIDER");
        when(provider.supportsRefund()).thenReturn(true);
        when(compensationTaskMapper.update(isNull(), any())).thenReturn(1);

        RefundOrder refundOrder = refundOrder("RA001", "PB001");
        when(refundOrderMapper.selectOne(any())).thenReturn(refundOrder);

        RefundRecord refundRecord = refundRecord("RA001");
        when(refundRecordMapper.selectOne(any())).thenReturn(refundRecord);
        when(paymentBillMapper.selectOne(any())).thenReturn(paymentBill("PB001", "SO1001", new BigDecimal("9.90")));

        RefundSubmitResultDTO result = new RefundSubmitResultDTO();
        result.setProviderRefundNo("PR001");
        result.setChannelStatus(RefundChannelStatusEnum.SUCCESS.name());
        result.setRawStatus("SUCCESS");
        result.setMessage("ok");
        when(provider.refund(any(), any())).thenReturn(result);

        RefundApplication application = refundApplication();
        when(refundApplicationMapper.selectOne(any())).thenReturn(application);

        RefundServiceImpl service = service(refundOrderMapper, refundRecordMapper, refundReconcileTaskMapper,
                compensationTaskMapper, paymentBillMapper, refundApplicationMapper, List.of(provider), refundApplicationService);

        CompensationTask task = task("MERCHANT_APPROVED_REFUND", "RA001");
        service.processLateCallbackRefundTask(task);

        assertEquals("SUCCESS", refundOrder.getRefundStatus());
        assertEquals(RefundChannelStatusEnum.SUCCESS.name(), refundRecord.getChannelStatus());
        verify(refundApplicationService).completeRefund(9L, 1L);
    }

    private RefundServiceImpl service(RefundOrderMapper refundOrderMapper,
                                      RefundRecordMapper refundRecordMapper,
                                      RefundReconcileTaskMapper refundReconcileTaskMapper,
                                      CompensationTaskMapper compensationTaskMapper,
                                      PaymentBillMapper paymentBillMapper,
                                      RefundApplicationMapper refundApplicationMapper,
                                      List<PaymentProvider> providers) {
        return new RefundServiceImpl(refundOrderMapper, refundRecordMapper, refundReconcileTaskMapper,
                compensationTaskMapper, paymentBillMapper, refundApplicationMapper,
                providers, mock(UserNotificationService.class), new CompensationTaskFactoryImpl(compensationTaskMapper),
                nullProvider());
    }

    private RefundServiceImpl service(RefundOrderMapper refundOrderMapper,
                                      RefundRecordMapper refundRecordMapper,
                                      RefundReconcileTaskMapper refundReconcileTaskMapper,
                                      CompensationTaskMapper compensationTaskMapper,
                                      PaymentBillMapper paymentBillMapper,
                                      RefundApplicationMapper refundApplicationMapper,
                                      List<PaymentProvider> providers,
                                      RefundApplicationService refundApplicationService) {
        return new RefundServiceImpl(refundOrderMapper, refundRecordMapper, refundReconcileTaskMapper,
                compensationTaskMapper, paymentBillMapper, refundApplicationMapper,
                providers, mock(UserNotificationService.class), new CompensationTaskFactoryImpl(compensationTaskMapper),
                providerOf(refundApplicationService));
    }

    private ObjectProvider<RefundApplicationService> nullProvider() {
        return providerOf(null);
    }

    private ObjectProvider<RefundApplicationService> providerOf(RefundApplicationService service) {
        @SuppressWarnings("unchecked")
        ObjectProvider<RefundApplicationService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(service);
        return provider;
    }

    private RefundOrder refundOrder(String refundNo, String paymentBillNo) {
        RefundOrder refundOrder = new RefundOrder();
        refundOrder.setRefundNo(refundNo);
        refundOrder.setPaymentBillNo(paymentBillNo);
        refundOrder.setChannelCode("EXT_PROVIDER");
        refundOrder.setExternalRefundAmount(new BigDecimal("9.90"));
        refundOrder.setRefundAmount(new BigDecimal("9.90"));
        refundOrder.setRefundStatus("APPLIED");
        refundOrder.setPlatformUserId(100L);
        return refundOrder;
    }

    private RefundRecord refundRecord(String refundNo) {
        RefundRecord refundRecord = new RefundRecord();
        refundRecord.setRefundNo(refundNo);
        refundRecord.setChannelCode("EXT_PROVIDER");
        return refundRecord;
    }

    private PaymentBill paymentBill(String billNo, String orderNo, BigDecimal payAmount) {
        PaymentBill paymentBill = new PaymentBill();
        paymentBill.setBillNo(billNo);
        paymentBill.setBizNo(orderNo);
        paymentBill.setTenantId(9L);
        paymentBill.setPlatformUserId(100L);
        paymentBill.setPayStatus(PayStatusEnum.SUCCESS.name());
        paymentBill.setChannelCode("EXT_PROVIDER");
        paymentBill.setPayAmount(payAmount);
        paymentBill.setThirdPartyBillNo("TP001");
        return paymentBill;
    }

    private CompensationTask task(String bizType, String bizNo) {
        CompensationTask task = new CompensationTask();
        task.setId(1L);
        task.setTaskNo("CT001");
        task.setBizType(bizType);
        task.setBizNo(bizNo);
        task.setTaskStatus("PENDING");
        task.setRetryCount(0);
        return task;
    }

    private RefundApplication refundApplication() {
        RefundApplication application = new RefundApplication();
        application.setId(1L);
        application.setRefundNo("RA001");
        application.setOrderNo("SO1001");
        application.setTenantId(9L);
        application.setPlatformUserId(100L);
        application.setRefundAmount(new BigDecimal("20.00"));
        application.setReason("不想要了");
        application.setAdminId(10L);
        application.setAuditTime(LocalDateTime.now());
        application.setRefundStatus(RefundApplicationStatus.APPROVED.name());
        return application;
    }
}
