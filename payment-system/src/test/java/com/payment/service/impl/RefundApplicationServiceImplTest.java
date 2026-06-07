package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.dto.RefundCreateDTO;
import com.payment.entity.RefundApplication;
import com.payment.entity.SalesOrder;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.RefundApplicationStatus;
import com.payment.mapper.ExchangeProductMapper;
import com.payment.mapper.RefundApplicationMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.PointsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefundApplicationServiceImplTest {

    private static final Long TENANT_ID = 9L;
    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 200L;
    private static final String ORDER_NO = "SO1001";

    @Test
    void testCreateRefund_正常创建() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        RefundApplicationServiceImpl service = new RefundApplicationServiceImpl(
                refundMapper, salesOrderMapper, mock(PointsService.class), mock(ExchangeProductMapper.class));

        when(salesOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidOrder());
        when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        RefundCreateDTO dto = refundDTO();
        RefundApplication result = service.createRefund(USER_ID, TENANT_ID, dto);

        ArgumentCaptor<RefundApplication> captor = ArgumentCaptor.forClass(RefundApplication.class);
        verify(refundMapper).insert(captor.capture());
        assertEquals(RefundApplicationStatus.PENDING.name(), captor.getValue().getRefundStatus());
        assertEquals(ORDER_NO, captor.getValue().getOrderNo());
        assertEquals(USER_ID, captor.getValue().getPlatformUserId());
        assertEquals(TENANT_ID, captor.getValue().getTenantId());
        assertEquals(new BigDecimal("20.00"), captor.getValue().getRefundAmount());
        assertNotNull(captor.getValue().getRefundNo());
        assertEquals(result, captor.getValue());
    }

    @Test
    void testCreateRefund_订单不存在抛异常() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        RefundApplicationServiceImpl service = new RefundApplicationServiceImpl(
                refundMapper, salesOrderMapper, mock(PointsService.class), mock(ExchangeProductMapper.class));

        when(salesOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> service.createRefund(USER_ID, TENANT_ID, refundDTO()));
    }

    @Test
    void testCreateRefund_非本人订单抛异常() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        RefundApplicationServiceImpl service = new RefundApplicationServiceImpl(
                refundMapper, salesOrderMapper, mock(PointsService.class), mock(ExchangeProductMapper.class));

        SalesOrder order = paidOrder();
        order.setPlatformUserId(OTHER_USER_ID);
        when(salesOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        assertThrows(BusinessException.class,
                () -> service.createRefund(USER_ID, TENANT_ID, refundDTO()));
    }

    @Test
    void testAuditRefund_批准退款() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        RefundApplicationServiceImpl service = new RefundApplicationServiceImpl(
                refundMapper, mock(SalesOrderMapper.class), mock(PointsService.class), mock(ExchangeProductMapper.class));

        when(refundMapper.selectById(1L)).thenReturn(pendingRefund());

        service.auditRefund(TENANT_ID, 1L, 10L, true, null);

        ArgumentCaptor<RefundApplication> captor = ArgumentCaptor.forClass(RefundApplication.class);
        verify(refundMapper).updateById(captor.capture());
        assertEquals(RefundApplicationStatus.APPROVED.name(), captor.getValue().getRefundStatus());
        assertEquals(10L, captor.getValue().getAdminId());
        assertNotNull(captor.getValue().getAuditTime());
    }

    @Test
    void testAuditRefund_拒绝退款需rejectReason() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        RefundApplicationServiceImpl service = new RefundApplicationServiceImpl(
                refundMapper, mock(SalesOrderMapper.class), mock(PointsService.class), mock(ExchangeProductMapper.class));

        when(refundMapper.selectById(1L)).thenReturn(pendingRefund());

        // rejectReason 为空时应抛异常
        assertThrows(BusinessException.class,
                () -> service.auditRefund(TENANT_ID, 1L, 10L, false, null));
        assertThrows(BusinessException.class,
                () -> service.auditRefund(TENANT_ID, 1L, 10L, false, "  "));

        // rejectReason 非空时应成功拒绝
        service.auditRefund(TENANT_ID, 1L, 10L, false, "商品不符合退款条件");

        ArgumentCaptor<RefundApplication> captor = ArgumentCaptor.forClass(RefundApplication.class);
        verify(refundMapper).updateById(captor.capture());
        assertEquals(RefundApplicationStatus.REJECTED.name(), captor.getValue().getRefundStatus());
        assertEquals("商品不符合退款条件", captor.getValue().getRejectReason());
    }

    @Test
    void testAuditRefund_非PENDING状态抛异常() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        RefundApplicationServiceImpl service = new RefundApplicationServiceImpl(
                refundMapper, mock(SalesOrderMapper.class), mock(PointsService.class), mock(ExchangeProductMapper.class));

        RefundApplication app = pendingRefund();
        app.setRefundStatus(RefundApplicationStatus.APPROVED.name());
        when(refundMapper.selectById(1L)).thenReturn(app);

        assertThrows(BusinessException.class,
                () -> service.auditRefund(TENANT_ID, 1L, 10L, true, null));
    }

    @Test
    void testCancelRefund_仅PENDING可取消() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        RefundApplicationServiceImpl service = new RefundApplicationServiceImpl(
                refundMapper, mock(SalesOrderMapper.class), mock(PointsService.class), mock(ExchangeProductMapper.class));

        when(refundMapper.selectById(1L)).thenReturn(pendingRefund());

        service.cancelRefund(USER_ID, TENANT_ID, 1L);

        ArgumentCaptor<RefundApplication> captor = ArgumentCaptor.forClass(RefundApplication.class);
        verify(refundMapper).updateById(captor.capture());
        assertEquals(RefundApplicationStatus.CANCELLED.name(), captor.getValue().getRefundStatus());

        // 非 PENDING 状态不可取消
        RefundApplication approved = pendingRefund();
        approved.setRefundStatus(RefundApplicationStatus.APPROVED.name());
        when(refundMapper.selectById(2L)).thenReturn(approved);

        assertThrows(BusinessException.class,
                () -> service.cancelRefund(USER_ID, TENANT_ID, 2L));
    }

    @Test
    void testCompleteRefund_积分兑换订单回退积分() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        PointsService pointsService = mock(PointsService.class);
        ExchangeProductMapper exchangeProductMapper = mock(ExchangeProductMapper.class);
        RefundApplicationServiceImpl service = new RefundApplicationServiceImpl(
                refundMapper, salesOrderMapper, pointsService, exchangeProductMapper);

        RefundApplication app = pendingRefund();
        app.setRefundStatus(RefundApplicationStatus.APPROVED.name());
        when(refundMapper.selectById(1L)).thenReturn(app);

        // 积分兑换订单
        SalesOrder exchangeOrder = new SalesOrder();
        exchangeOrder.setOrderNo(ORDER_NO);
        exchangeOrder.setTenantId(TENANT_ID);
        exchangeOrder.setSource("EXCHANGE");
        exchangeOrder.setPointsDeductAmount(new BigDecimal("150"));
        when(salesOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(exchangeOrder);

        service.completeRefund(TENANT_ID, 1L);

        // 验证状态变为 COMPLETED
        ArgumentCaptor<RefundApplication> appCaptor = ArgumentCaptor.forClass(RefundApplication.class);
        verify(refundMapper).updateById(appCaptor.capture());
        assertEquals(RefundApplicationStatus.COMPLETED.name(), appCaptor.getValue().getRefundStatus());
        assertNotNull(appCaptor.getValue().getCompleteTime());

        // 验证积分回退
        verify(pointsService).refundPoints(USER_ID, TENANT_ID, 150, ORDER_NO,
                "积分兑换商品退款回退，退款单号：" + app.getRefundNo());
    }

    @Test
    void testCompleteRefund_非积分订单不回退积分() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        PointsService pointsService = mock(PointsService.class);
        RefundApplicationServiceImpl service = new RefundApplicationServiceImpl(
                refundMapper, salesOrderMapper, pointsService, mock(ExchangeProductMapper.class));

        RefundApplication app = pendingRefund();
        app.setRefundStatus(RefundApplicationStatus.APPROVED.name());
        when(refundMapper.selectById(1L)).thenReturn(app);

        // 普通订单（source 非 EXCHANGE，orderNo 不以 EX 开头）
        SalesOrder normalOrder = new SalesOrder();
        normalOrder.setOrderNo(ORDER_NO);
        normalOrder.setTenantId(TENANT_ID);
        normalOrder.setSource("NORMAL");
        when(salesOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(normalOrder);

        service.completeRefund(TENANT_ID, 1L);

        verify(pointsService, never()).refundPoints(anyLong(), anyLong(), any(), any(), any());
    }

    // ---- helper methods ----

    private SalesOrder paidOrder() {
        SalesOrder order = new SalesOrder();
        order.setId(1L);
        order.setOrderNo(ORDER_NO);
        order.setTenantId(TENANT_ID);
        order.setPlatformUserId(USER_ID);
        order.setOrderStatus(OrderStatusEnum.PAID.name());
        order.setPayableAmount(new BigDecimal("100.00"));
        return order;
    }

    private RefundApplication pendingRefund() {
        RefundApplication app = new RefundApplication();
        app.setId(1L);
        app.setRefundNo("RA202606070001");
        app.setOrderNo(ORDER_NO);
        app.setPlatformUserId(USER_ID);
        app.setTenantId(TENANT_ID);
        app.setRefundType("REFUND_ONLY");
        app.setRefundStatus(RefundApplicationStatus.PENDING.name());
        app.setRefundAmount(new BigDecimal("20.00"));
        app.setReason("不想要了");
        return app;
    }

    private RefundCreateDTO refundDTO() {
        RefundCreateDTO dto = new RefundCreateDTO();
        dto.setOrderNo(ORDER_NO);
        dto.setRefundType("REFUND_ONLY");
        dto.setRefundAmount(new BigDecimal("20.00"));
        dto.setReason("不想要了");
        return dto;
    }
}
