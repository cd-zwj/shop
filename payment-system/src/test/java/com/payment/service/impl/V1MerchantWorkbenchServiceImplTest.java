package com.payment.service.impl;

import com.payment.common.PageResult;
import com.payment.dto.MerchantWorkbenchTaskVO;
import com.payment.dto.MerchantWorkbenchTodoSummaryVO;
import com.payment.entity.CompensationTask;
import com.payment.entity.RetryTask;
import com.payment.mapper.CompensationTaskMapper;
import com.payment.mapper.OrderDeliveryRecordMapper;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.RefundApplicationMapper;
import com.payment.mapper.RetryTaskMapper;
import com.payment.mapper.SalesOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class V1MerchantWorkbenchServiceImplTest {

    private SalesOrderMapper salesOrderMapper;
    private RefundApplicationMapper refundApplicationMapper;
    private OrderDeliveryRecordMapper orderDeliveryRecordMapper;
    private ProductMapper productMapper;
    private CompensationTaskMapper compensationTaskMapper;
    private RetryTaskMapper retryTaskMapper;
    private V1MerchantWorkbenchServiceImpl service;

    @BeforeEach
    void setUp() {
        salesOrderMapper = mock(SalesOrderMapper.class);
        refundApplicationMapper = mock(RefundApplicationMapper.class);
        orderDeliveryRecordMapper = mock(OrderDeliveryRecordMapper.class);
        productMapper = mock(ProductMapper.class);
        compensationTaskMapper = mock(CompensationTaskMapper.class);
        retryTaskMapper = mock(RetryTaskMapper.class);
        service = new V1MerchantWorkbenchServiceImpl(
                salesOrderMapper, refundApplicationMapper, orderDeliveryRecordMapper, productMapper,
                compensationTaskMapper, retryTaskMapper);
    }

    @Test
    void todoSummaryShouldAggregateMerchantActionCounts() {
        when(salesOrderMapper.selectCount(any()))
                .thenReturn(2L)
                .thenReturn(1L);
        when(salesOrderMapper.countAbnormalOrdersByTenant(9L)).thenReturn(4L);
        when(orderDeliveryRecordMapper.countDistinctOrdersByTenantAndStatuses(eq(9L), eq(List.of("PENDING", "DELIVERING"))))
                .thenReturn(3L);
        when(refundApplicationMapper.selectCount(any()))
                .thenReturn(5L)
                .thenReturn(6L);
        when(productMapper.countActiveLowStockByTenant(9L, 5)).thenReturn(7L);
        when(compensationTaskMapper.countMerchantVisibleOpenTasks(9L)).thenReturn(8L);
        when(retryTaskMapper.countMerchantVisibleOpenTasks(9L)).thenReturn(9L);

        MerchantWorkbenchTodoSummaryVO summary = service.getTodoSummary(9L);

        assertEquals(44L, summary.getTotalCount());
        assertEquals(List.of(
                "payment", "fulfillment", "abnormalOrder", "refund", "refundFailed", "compensation", "retry", "stock"),
                summary.getItems().stream().map(item -> item.getKey()).toList());
        assertEquals(2L, summary.getItems().get(0).getCount());
        assertEquals(3L, summary.getItems().get(1).getCount());
        assertEquals(4L, summary.getItems().get(2).getCount());
        assertEquals(5L, summary.getItems().get(3).getCount());
        assertEquals(6L, summary.getItems().get(4).getCount());
        assertEquals(8L, summary.getItems().get(5).getCount());
        assertEquals(9L, summary.getItems().get(6).getCount());
        assertEquals(7L, summary.getItems().get(7).getCount());
        assertEquals("/merchant/orders?tab=shipping", summary.getItems().get(1).getPath());
        assertEquals("red", summary.getItems().get(2).getTone());
        assertEquals("/merchant/tasks?type=compensation", summary.getItems().get(5).getPath());
        assertEquals("/merchant/tasks?type=retry", summary.getItems().get(6).getPath());

        verify(productMapper).countActiveLowStockByTenant(9L, 5);
        verify(compensationTaskMapper).countMerchantVisibleOpenTasks(9L);
        verify(retryTaskMapper).countMerchantVisibleOpenTasks(9L);
    }

    @Test
    void listVisibleTasksShouldExposeCompensationTasksForMerchantReadOnlyView() {
        CompensationTask task = new CompensationTask();
        task.setId(11L);
        task.setTaskNo("CT202607080001");
        task.setBizType("MERCHANT_APPROVED_REFUND");
        task.setBizNo("RA202607080001");
        task.setTaskStatus("FAIL");
        task.setRemark("Provider refund is not supported in phase 1");
        task.setRetryCount(5);
        task.setCreateTime(LocalDateTime.of(2026, 7, 8, 10, 0));
        task.setUpdateTime(LocalDateTime.of(2026, 7, 8, 10, 5));
        when(compensationTaskMapper.countMerchantVisibleOpenTasks(9L)).thenReturn(1L);
        when(compensationTaskMapper.selectMerchantVisibleOpenTasks(9L, 20, 0)).thenReturn(List.of(task));

        PageResult<MerchantWorkbenchTaskVO> page = service.listVisibleTasks(9L, "compensation", 1, 20);

        assertEquals(1L, page.getTotal());
        assertEquals("compensation", page.getRecords().get(0).getTaskSource());
        assertEquals("CT202607080001", page.getRecords().get(0).getTaskNo());
        assertEquals("MERCHANT_APPROVED_REFUND", page.getRecords().get(0).getBizType());
        assertEquals("RA202607080001", page.getRecords().get(0).getBizNo());
        assertEquals("FAIL", page.getRecords().get(0).getTaskStatus());
        assertEquals("Provider refund is not supported in phase 1", page.getRecords().get(0).getLastError());
        assertEquals("查看退款单", page.getRecords().get(0).getActionLabel());
        assertEquals("/merchant/refunds?status=FAILED", page.getRecords().get(0).getActionPath());
    }

    @Test
    void listVisibleTasksShouldExposeRetryTasksForMerchantReadOnlyView() {
        RetryTask task = new RetryTask();
        task.setId(21L);
        task.setTaskNo("RT202607080001");
        task.setTaskType("ORDER_CLOSE");
        task.setBizType("ORDER");
        task.setBizNo("SO202607080001");
        task.setTaskStatus("DEAD");
        task.setRetryCount(16);
        task.setMaxRetryCount(16);
        task.setNextRetryTime(LocalDateTime.of(2026, 7, 8, 11, 0));
        task.setLastErrorMessage("Order close failed");
        task.setCreateTime(LocalDateTime.of(2026, 7, 8, 10, 0));
        task.setUpdateTime(LocalDateTime.of(2026, 7, 8, 10, 5));
        when(retryTaskMapper.countMerchantVisibleOpenTasks(9L)).thenReturn(1L);
        when(retryTaskMapper.selectMerchantVisibleOpenTasks(9L, 10, 10)).thenReturn(List.of(task));

        PageResult<MerchantWorkbenchTaskVO> page = service.listVisibleTasks(9L, "retry", 2, 10);

        assertEquals(1L, page.getTotal());
        assertEquals(2, page.getPage());
        assertEquals("retry", page.getRecords().get(0).getTaskSource());
        assertEquals("ORDER_CLOSE", page.getRecords().get(0).getTaskType());
        assertEquals("DEAD", page.getRecords().get(0).getTaskStatus());
        assertEquals("Order close failed", page.getRecords().get(0).getLastError());
        assertEquals("查看异常订单", page.getRecords().get(0).getActionLabel());
        assertEquals("/merchant/orders?tab=abnormal", page.getRecords().get(0).getActionPath());
    }

    @Test
    void todoSummaryShouldTreatNullCountsAsZero() {
        when(salesOrderMapper.selectCount(any())).thenReturn(null);
        when(salesOrderMapper.countAbnormalOrdersByTenant(9L)).thenReturn(null);
        when(orderDeliveryRecordMapper.countDistinctOrdersByTenantAndStatuses(any(), any())).thenReturn(null);
        when(refundApplicationMapper.selectCount(any())).thenReturn(null);
        when(productMapper.countActiveLowStockByTenant(9L, 5)).thenReturn(null);
        when(compensationTaskMapper.countMerchantVisibleOpenTasks(9L)).thenReturn(null);
        when(retryTaskMapper.countMerchantVisibleOpenTasks(9L)).thenReturn(null);

        MerchantWorkbenchTodoSummaryVO summary = service.getTodoSummary(9L);

        assertEquals(0L, summary.getTotalCount());
        assertEquals(8, summary.getItems().size());
        summary.getItems().forEach(item -> assertEquals(0L, item.getCount()));
    }
}
