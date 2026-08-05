package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.dto.RefundCreateDTO;
import com.payment.entity.OrderDeliveryRecord;
import com.payment.entity.RefundApplication;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.DeliveryStatusEnum;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.RefundApplicationStatus;
import com.payment.mapper.AfterSaleActionMapper;
import com.payment.mapper.RefundApplicationMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.RefundService;
import com.payment.service.UserNotificationService;
import com.payment.service.delivery.OrderDeliveryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RefundApplicationServiceImplTest {

    private static final Long TENANT_ID = 9L;
    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 200L;
    private static final String ORDER_NO = "SO1001";
    private static final Long ORDER_ITEM_ID = 501L;

    @Test
    void testCreateRefund_正常创建并保留待商家审核() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper itemMapper = mock(SalesOrderItemMapper.class);
        RefundApplicationServiceImpl service = service(refundMapper, salesOrderMapper, itemMapper);

        when(salesOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidOrder());
        when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(refundMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(itemMapper.selectByOrderNo(ORDER_NO)).thenReturn(List.of(orderItem(DeliveryStatusEnum.PENDING.name())));

        RefundCreateDTO dto = refundDTO();
        RefundApplication result = service.createRefund(USER_ID, TENANT_ID, dto);

        ArgumentCaptor<RefundApplication> captor = ArgumentCaptor.forClass(RefundApplication.class);
        verify(refundMapper).insert(captor.capture());
        assertEquals(RefundApplicationStatus.PENDING.name(), captor.getValue().getRefundStatus());
        assertEquals(ORDER_NO, captor.getValue().getOrderNo());
        assertEquals(USER_ID, captor.getValue().getPlatformUserId());
        assertEquals(TENANT_ID, captor.getValue().getTenantId());
        assertEquals(new BigDecimal("20.00"), captor.getValue().getRefundAmount());
        assertEquals(DeliveryStatusEnum.PENDING.name(), captor.getValue().getDeliveryStatus());
        assertEquals(Boolean.TRUE, captor.getValue().getQuickRefundSuggested());
        assertNotNull(captor.getValue().getRefundNo());
        assertEquals(result, captor.getValue());
    }

    @Test
    void testCreateRefund_订单不存在抛异常() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        RefundApplicationServiceImpl service = service(refundMapper, salesOrderMapper, mock(SalesOrderItemMapper.class));

        when(salesOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> service.createRefund(USER_ID, TENANT_ID, refundDTO()));
    }

    @Test
    void testCreateRefund_非本人订单抛异常() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        RefundApplicationServiceImpl service = service(refundMapper, salesOrderMapper, mock(SalesOrderItemMapper.class));

        SalesOrder order = paidOrder();
        order.setPlatformUserId(OTHER_USER_ID);
        when(salesOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        assertThrows(BusinessException.class,
                () -> service.createRefund(USER_ID, TENANT_ID, refundDTO()));
    }

    @Test
    void testCreateRefund_重复活跃退款被拦截() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        RefundApplicationServiceImpl service = service(refundMapper, salesOrderMapper, mock(SalesOrderItemMapper.class));

        when(salesOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidOrder());
        when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class,
                () -> service.createRefund(USER_ID, TENANT_ID, refundDTO()));
    }

    @Test
    void testCreateRefund_同单不同订单项允许分别申请() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper itemMapper = mock(SalesOrderItemMapper.class);
        RefundApplicationServiceImpl service = service(refundMapper, salesOrderMapper, itemMapper);

        when(salesOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidOrder());
        when(itemMapper.selectById(ORDER_ITEM_ID)).thenReturn(orderItem(DeliveryStatusEnum.PENDING.name()));
        when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(refundMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        RefundCreateDTO dto = refundDTO();
        dto.setOrderItemId(ORDER_ITEM_ID);

        assertDoesNotThrow(() -> service.createRefund(USER_ID, TENANT_ID, dto));
    }

    @Test
    void testCreateRefund_退货退款缺订单项被拦截() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        RefundApplicationServiceImpl service = service(refundMapper, salesOrderMapper, mock(SalesOrderItemMapper.class));

        when(salesOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidOrder());

        // 退货退款必须关联订单项，否则退款完成后无法回补门店库存
        RefundCreateDTO dto = refundDTO();
        dto.setRefundType("RETURN_REFUND");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createRefund(USER_ID, TENANT_ID, dto));
        assertEquals("退货退款需选择具体商品", exception.getMessage());
        verify(refundMapper, never()).insert(any(RefundApplication.class));
    }

    @Test
    void testCreateRefund_超过可退余额被拦截() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper itemMapper = mock(SalesOrderItemMapper.class);
        RefundApplicationServiceImpl service = service(refundMapper, salesOrderMapper, itemMapper);

        RefundApplication completed = pendingRefund();
        completed.setRefundStatus(RefundApplicationStatus.COMPLETED.name());
        completed.setRefundAmount(new BigDecimal("90.00"));

        when(salesOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidOrder());
        when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(refundMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(completed));

        assertThrows(BusinessException.class,
                () -> service.createRefund(USER_ID, TENANT_ID, refundDTO()));
    }

    @Test
    void testCreateRefund_单项退款超过订单项金额被拦截() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper itemMapper = mock(SalesOrderItemMapper.class);
        RefundApplicationServiceImpl service = service(refundMapper, salesOrderMapper, itemMapper);

        SalesOrderItem item = orderItem(DeliveryStatusEnum.PENDING.name());
        item.setSubtotal(new BigDecimal("10.00"));
        when(salesOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidOrder());
        when(itemMapper.selectById(ORDER_ITEM_ID)).thenReturn(item);
        when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(refundMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        RefundCreateDTO dto = refundDTO();
        dto.setOrderItemId(ORDER_ITEM_ID);
        dto.setRefundAmount(new BigDecimal("20.00"));

        assertThrows(BusinessException.class,
                () -> service.createRefund(USER_ID, TENANT_ID, dto));
    }

    @Test
    void testAuditRefund_未发货批准后进入处理且不撤销交付() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderItemMapper itemMapper = mock(SalesOrderItemMapper.class);
        OrderDeliveryService deliveryService = mock(OrderDeliveryService.class);
        RefundService refundService = mock(RefundService.class);
        RefundApplicationServiceImpl service = service(refundMapper, mock(SalesOrderMapper.class),
                itemMapper, deliveryService, refundService);
        List<String> statuses = captureUpdateStatuses(refundMapper);

        when(refundMapper.selectById(1L)).thenReturn(pendingRefund());
        when(itemMapper.selectByOrderNo(ORDER_NO)).thenReturn(List.of(orderItem(DeliveryStatusEnum.PENDING.name())));

        service.auditRefund(TENANT_ID, 1L, 10L, true, null);

        ArgumentCaptor<RefundApplication> captor = ArgumentCaptor.forClass(RefundApplication.class);
        verify(refundMapper).updateById(captor.capture());
        assertEquals(List.of(RefundApplicationStatus.PROCESSING.name()), statuses);
        RefundApplication finalUpdate = captor.getValue();
        assertEquals(RefundApplicationStatus.PROCESSING.name(), finalUpdate.getRefundStatus());
        assertEquals(DeliveryStatusEnum.PENDING.name(), finalUpdate.getDeliveryStatus());
        assertEquals(Boolean.TRUE, finalUpdate.getQuickRefundSuggested());
        assertEquals(10L, finalUpdate.getAdminId());
        assertNotNull(finalUpdate.getAuditTime());
        verify(deliveryService, never()).revokeByOrderNo(any());
        verify(refundService).prepareMerchantApprovedRefund(finalUpdate);
    }

    @Test
    void testAuditRefund_已交付批准时先撤销再退款() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderItemMapper itemMapper = mock(SalesOrderItemMapper.class);
        OrderDeliveryService deliveryService = mock(OrderDeliveryService.class);
        RefundService refundService = mock(RefundService.class);
        RefundApplicationServiceImpl service = service(refundMapper, mock(SalesOrderMapper.class),
                itemMapper, deliveryService, refundService);
        List<String> statuses = captureUpdateStatuses(refundMapper);

        RefundApplication app = pendingRefund();
        app.setOrderItemId(ORDER_ITEM_ID);
        when(refundMapper.selectById(1L)).thenReturn(app);
        when(itemMapper.selectById(ORDER_ITEM_ID)).thenReturn(orderItem(DeliveryStatusEnum.DELIVERED.name()));
        when(deliveryService.revokeByOrderItem(ORDER_ITEM_ID)).thenReturn(List.of(deliveryRecord(DeliveryStatusEnum.REVOKED.name())));

        service.auditRefund(TENANT_ID, 1L, 10L, true, null);

        InOrder inOrder = inOrder(deliveryService, refundService);
        inOrder.verify(deliveryService).revokeByOrderItem(ORDER_ITEM_ID);
        inOrder.verify(refundService).prepareMerchantApprovedRefund(app);

        ArgumentCaptor<RefundApplication> captor = ArgumentCaptor.forClass(RefundApplication.class);
        verify(refundMapper).updateById(captor.capture());
        assertEquals(List.of(RefundApplicationStatus.PROCESSING.name()), statuses);
        assertEquals(RefundApplicationStatus.PROCESSING.name(), captor.getValue().getRefundStatus());
        assertEquals(Boolean.FALSE, captor.getValue().getQuickRefundSuggested());
    }

    @Test
    void testIntervene_商家驳回后平台可同意且仅记录平台操作() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper itemMapper = mock(SalesOrderItemMapper.class);
        OrderDeliveryService deliveryService = mock(OrderDeliveryService.class);
        RefundService refundService = mock(RefundService.class);
        AfterSaleActionMapper actionMapper = mock(AfterSaleActionMapper.class);
        RefundApplicationServiceImpl service = service(refundMapper, salesOrderMapper, itemMapper,
                deliveryService, refundService, actionMapper);

        RefundApplication app = pendingRefund();
        app.setRefundStatus(RefundApplicationStatus.REJECTED.name());
        app.setRejectReason("商家不同意");
        when(refundMapper.selectById(1L)).thenReturn(app);
        when(salesOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidOrder());
        when(refundMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(itemMapper.selectByOrderNo(ORDER_NO)).thenReturn(List.of(orderItem(DeliveryStatusEnum.PENDING.name())));

        when(refundMapper.claimDecision(eq(1L), eq(TENANT_ID), eq(RefundApplicationStatus.REJECTED.name()),
                eq(RefundApplicationStatus.APPROVED.name()), eq(99L), isNull())).thenReturn(1);

        service.intervene(TENANT_ID, 1L, 99L, RefundApplicationStatus.REJECTED.name(),
                true, "核实凭证后支持退款");

        verify(refundService).prepareMerchantApprovedRefund(app);
        ArgumentCaptor<com.payment.entity.AfterSaleAction> actionCaptor =
                ArgumentCaptor.forClass(com.payment.entity.AfterSaleAction.class);
        verify(actionMapper).insert(actionCaptor.capture());
        assertEquals("PLATFORM_APPROVE", actionCaptor.getValue().getAction());
        assertEquals("ADMIN", actionCaptor.getValue().getOperatorRole());
        assertEquals("核实凭证后支持退款", actionCaptor.getValue().getRemark());
        assertEquals(RefundApplicationStatus.PROCESSING.name(), app.getRefundStatus());
        InOrder lockOrder = inOrder(salesOrderMapper, refundMapper);
        lockOrder.verify(salesOrderMapper).selectOne(any(LambdaQueryWrapper.class));
        lockOrder.verify(refundMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void testIntervene_已驳回申请不允许重复驳回覆盖原决定() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        RefundApplicationServiceImpl service = service(
                refundMapper, mock(SalesOrderMapper.class), mock(SalesOrderItemMapper.class));
        RefundApplication app = pendingRefund();
        app.setRefundStatus(RefundApplicationStatus.REJECTED.name());
        when(refundMapper.selectById(1L)).thenReturn(app);

        assertThrows(BusinessException.class, () -> service.intervene(
                TENANT_ID, 1L, 99L, RefundApplicationStatus.REJECTED.name(), false, "再次驳回"));

        verify(refundMapper, never()).claimDecision(anyLong(), anyLong(), any(), any(), anyLong(), any());
    }

    @Test
    void testListAdminRefunds_跨租户筛选交给专用分页查询() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper itemMapper = mock(SalesOrderItemMapper.class);
        RefundApplicationServiceImpl service = service(
                refundMapper, salesOrderMapper, itemMapper);
        Page<RefundApplication> expected = new Page<>(2, 20);
        expected.setRecords(List.of(pendingRefund()));
        expected.setTotal(1);
        when(refundMapper.selectAdminPage(any(Page.class), eq(TENANT_ID),
                eq(RefundApplicationStatus.PENDING.name()), eq("RA-100"))).thenReturn(expected);

        Page<RefundApplication> result = service.listAdminRefunds(
                TENANT_ID, RefundApplicationStatus.PENDING.name(), " RA-100 ", 2, 20);

        assertEquals(expected, result);
        verify(refundMapper).selectAdminPage(any(Page.class), eq(TENANT_ID),
                eq(RefundApplicationStatus.PENDING.name()), eq("RA-100"));
        verifyNoInteractions(salesOrderMapper, itemMapper);
    }

    @Test
    void testListAdminRefunds_非法状态被拒绝() {
        RefundApplicationServiceImpl service = service(
                mock(RefundApplicationMapper.class), mock(SalesOrderMapper.class), mock(SalesOrderItemMapper.class));

        assertThrows(BusinessException.class,
                () -> service.listAdminRefunds(null, "UNKNOWN", null, 1, 20));
    }

    @Test
    void testGetAdminRefund_租户与退款单不匹配时不返回数据() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        RefundApplicationServiceImpl service = service(
                refundMapper, mock(SalesOrderMapper.class), mock(SalesOrderItemMapper.class));
        when(refundMapper.selectById(1L)).thenReturn(pendingRefund());

        assertThrows(BusinessException.class, () -> service.getAdminRefund(99L, 1L));
    }

    @Test
    void testIntervene_旧状态提交在CAS失败后不得触发退款或流水() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        RefundService refundService = mock(RefundService.class);
        AfterSaleActionMapper actionMapper = mock(AfterSaleActionMapper.class);
        RefundApplicationServiceImpl service = service(
                refundMapper, mock(SalesOrderMapper.class), mock(SalesOrderItemMapper.class),
                mock(OrderDeliveryService.class), refundService, actionMapper);
        when(refundMapper.selectById(1L)).thenReturn(pendingRefund());
        when(refundMapper.claimDecision(eq(1L), eq(TENANT_ID), eq(RefundApplicationStatus.PENDING.name()),
                eq(RefundApplicationStatus.APPROVED.name()), eq(99L), isNull())).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.intervene(
                TENANT_ID, 1L, 99L, RefundApplicationStatus.PENDING.name(), true, "同意退款"));

        verify(refundService, never()).prepareMerchantApprovedRefund(any());
        verify(actionMapper, never()).insert(any(com.payment.entity.AfterSaleAction.class));
    }

    @Test
    void testIntervene_说明超长在读库前被拒绝() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        RefundApplicationServiceImpl service = service(
                refundMapper, mock(SalesOrderMapper.class), mock(SalesOrderItemMapper.class));

        assertThrows(BusinessException.class, () -> service.intervene(
                TENANT_ID, 1L, 99L, RefundApplicationStatus.PENDING.name(), true, "a".repeat(1001)));
        verify(refundMapper, never()).selectById(anyLong());
    }

    @Test
    void testAuditRefund_交付撤销失败时进入失败状态且不退款() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderItemMapper itemMapper = mock(SalesOrderItemMapper.class);
        OrderDeliveryService deliveryService = mock(OrderDeliveryService.class);
        RefundService refundService = mock(RefundService.class);
        RefundApplicationServiceImpl service = service(refundMapper, mock(SalesOrderMapper.class),
                itemMapper, deliveryService, refundService);
        List<String> statuses = captureUpdateStatuses(refundMapper);

        RefundApplication app = pendingRefund();
        app.setOrderItemId(ORDER_ITEM_ID);
        when(refundMapper.selectById(1L)).thenReturn(app);
        when(itemMapper.selectById(ORDER_ITEM_ID)).thenReturn(orderItem(DeliveryStatusEnum.CONFIRMED.name()));
        when(deliveryService.revokeByOrderItem(ORDER_ITEM_ID)).thenReturn(List.of(deliveryRecord(DeliveryStatusEnum.REVOKE_FAILED.name())));

        service.auditRefund(TENANT_ID, 1L, 10L, true, null);

        ArgumentCaptor<RefundApplication> captor = ArgumentCaptor.forClass(RefundApplication.class);
        verify(refundMapper).updateById(captor.capture());
        assertEquals(List.of(RefundApplicationStatus.FAILED.name()), statuses);
        RefundApplication finalUpdate = captor.getValue();
        assertEquals(RefundApplicationStatus.FAILED.name(), finalUpdate.getRefundStatus());
        assertEquals("交付撤销失败，请人工处理后再退款", finalUpdate.getRejectReason());
        verify(refundService, never()).prepareMerchantApprovedRefund(any());
    }

    @Test
    void testAuditRefund_拒绝退款需rejectReason() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        RefundApplicationServiceImpl service = service(refundMapper, mock(SalesOrderMapper.class), mock(SalesOrderItemMapper.class));

        when(refundMapper.selectById(1L)).thenReturn(pendingRefund());

        assertThrows(BusinessException.class,
                () -> service.auditRefund(TENANT_ID, 1L, 10L, false, null));
        assertThrows(BusinessException.class,
                () -> service.auditRefund(TENANT_ID, 1L, 10L, false, "  "));

        service.auditRefund(TENANT_ID, 1L, 10L, false, "商品不符合退款条件");

        verify(refundMapper).claimDecision(1L, TENANT_ID, RefundApplicationStatus.PENDING.name(),
                RefundApplicationStatus.REJECTED.name(), 10L, "商品不符合退款条件");
        verify(refundMapper, never()).updateById(any(RefundApplication.class));
    }

    @Test
    void testAuditRefund_非PENDING状态抛异常() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        RefundApplicationServiceImpl service = service(refundMapper, mock(SalesOrderMapper.class), mock(SalesOrderItemMapper.class));

        RefundApplication app = pendingRefund();
        app.setRefundStatus(RefundApplicationStatus.APPROVED.name());
        when(refundMapper.selectById(1L)).thenReturn(app);

        assertThrows(BusinessException.class,
                () -> service.auditRefund(TENANT_ID, 1L, 10L, true, null));
    }

    @Test
    void testCancelRefund_仅PENDING可取消() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        AfterSaleActionMapper actionMapper = mock(AfterSaleActionMapper.class);
        RefundApplicationServiceImpl service = service(
                refundMapper, mock(SalesOrderMapper.class), mock(SalesOrderItemMapper.class),
                mock(OrderDeliveryService.class), mock(RefundService.class), actionMapper);

        when(refundMapper.selectById(1L)).thenReturn(pendingRefund());
        when(refundMapper.cancelPending(1L, TENANT_ID, USER_ID)).thenReturn(1);

        service.cancelRefund(USER_ID, TENANT_ID, 1L);

        verify(refundMapper).cancelPending(1L, TENANT_ID, USER_ID);
        verify(refundMapper, never()).updateById(any(RefundApplication.class));
        ArgumentCaptor<com.payment.entity.AfterSaleAction> actionCaptor =
                ArgumentCaptor.forClass(com.payment.entity.AfterSaleAction.class);
        verify(actionMapper).insert(actionCaptor.capture());
        assertEquals("USER_CANCEL", actionCaptor.getValue().getAction());

        RefundApplication approved = pendingRefund();
        approved.setRefundStatus(RefundApplicationStatus.APPROVED.name());
        when(refundMapper.selectById(2L)).thenReturn(approved);

        assertThrows(BusinessException.class,
                () -> service.cancelRefund(USER_ID, TENANT_ID, 2L));
    }

    @Test
    void testCancelRefund_CAS失败不得写取消流水() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        AfterSaleActionMapper actionMapper = mock(AfterSaleActionMapper.class);
        RefundApplicationServiceImpl service = service(
                refundMapper, mock(SalesOrderMapper.class), mock(SalesOrderItemMapper.class),
                mock(OrderDeliveryService.class), mock(RefundService.class), actionMapper);
        when(refundMapper.selectById(1L)).thenReturn(pendingRefund());
        when(refundMapper.cancelPending(1L, TENANT_ID, USER_ID)).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.cancelRefund(USER_ID, TENANT_ID, 1L));

        verify(actionMapper, never()).insert(any(com.payment.entity.AfterSaleAction.class));
        verify(refundMapper, never()).updateById(any(RefundApplication.class));
    }

    @Test
    void testCancelRefund_非本人或非本租户不得触发CAS() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        AfterSaleActionMapper actionMapper = mock(AfterSaleActionMapper.class);
        RefundApplicationServiceImpl service = service(
                refundMapper, mock(SalesOrderMapper.class), mock(SalesOrderItemMapper.class),
                mock(OrderDeliveryService.class), mock(RefundService.class), actionMapper);
        when(refundMapper.selectById(1L)).thenReturn(pendingRefund());

        assertThrows(BusinessException.class,
                () -> service.cancelRefund(OTHER_USER_ID, TENANT_ID, 1L));
        assertThrows(BusinessException.class,
                () -> service.cancelRefund(USER_ID, 99L, 1L));

        verify(refundMapper, never()).cancelPending(anyLong(), anyLong(), anyLong());
        verify(actionMapper, never()).insert(any(com.payment.entity.AfterSaleAction.class));
    }

    @Test
    void testCompleteRefund_已完成且有完成时间时幂等跳过() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        OrderDeliveryService deliveryService = mock(OrderDeliveryService.class);
        RefundApplicationServiceImpl service = service(refundMapper, mock(SalesOrderMapper.class),
                mock(SalesOrderItemMapper.class), deliveryService, mock(RefundService.class));

        RefundApplication app = pendingRefund();
        app.setRefundStatus(RefundApplicationStatus.COMPLETED.name());
        app.setCompleteTime(LocalDateTime.now());
        when(refundMapper.selectById(1L)).thenReturn(app);

        service.completeRefund(TENANT_ID, 1L);

        verify(refundMapper, never()).updateById(any(RefundApplication.class));
        verify(deliveryService, never()).revokeByOrderNo(any());
    }

    private RefundApplicationServiceImpl service(RefundApplicationMapper refundMapper,
                                                SalesOrderMapper salesOrderMapper,
                                                SalesOrderItemMapper itemMapper) {
        return service(refundMapper, salesOrderMapper, itemMapper,
                mock(OrderDeliveryService.class), mock(RefundService.class));
    }

    private RefundApplicationServiceImpl service(RefundApplicationMapper refundMapper,
                                                SalesOrderMapper salesOrderMapper,
                                                SalesOrderItemMapper itemMapper,
                                                OrderDeliveryService deliveryService,
                                                RefundService refundService) {
        return service(refundMapper, salesOrderMapper, itemMapper, deliveryService, refundService,
                mock(AfterSaleActionMapper.class));
    }

    private RefundApplicationServiceImpl service(RefundApplicationMapper refundMapper,
                                                SalesOrderMapper salesOrderMapper,
                                                SalesOrderItemMapper itemMapper,
                                                OrderDeliveryService deliveryService,
                                                RefundService refundService,
                                                AfterSaleActionMapper afterSaleActionMapper) {
        org.mockito.Mockito.lenient().when(refundMapper.claimDecision(
                        anyLong(), anyLong(), any(), any(), anyLong(), any()))
                .thenReturn(1);
        return new RefundApplicationServiceImpl(refundMapper, salesOrderMapper, itemMapper,
                mock(UserNotificationService.class), deliveryService, refundService,
                mock(com.payment.service.StoreInventoryService.class), afterSaleActionMapper,
                mock(MerchantStoreScopeService.class));
    }

    private List<String> captureUpdateStatuses(RefundApplicationMapper refundMapper) {
        List<String> statuses = new ArrayList<>();
        when(refundMapper.updateById(any(RefundApplication.class))).thenAnswer(invocation -> {
            RefundApplication app = invocation.getArgument(0);
            statuses.add(app.getRefundStatus());
            return 1;
        });
        return statuses;
    }

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

    private SalesOrderItem orderItem(String deliveryStatus) {
        SalesOrderItem item = new SalesOrderItem();
        item.setId(ORDER_ITEM_ID);
        item.setOrderId(1L);
        item.setOrderNo(ORDER_NO);
        item.setTenantId(TENANT_ID);
        item.setDeliveryStatus(deliveryStatus);
        item.setSubtotal(new BigDecimal("20.00"));
        return item;
    }

    private OrderDeliveryRecord deliveryRecord(String status) {
        OrderDeliveryRecord record = new OrderDeliveryRecord();
        record.setOrderItemId(ORDER_ITEM_ID);
        record.setOrderNo(ORDER_NO);
        record.setStatus(status);
        return record;
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
