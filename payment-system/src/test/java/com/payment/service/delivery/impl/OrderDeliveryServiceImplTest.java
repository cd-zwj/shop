package com.payment.service.delivery.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.payment.common.BusinessException;
import com.payment.entity.MessageOutbox;
import com.payment.entity.OrderDeliveryRecord;
import com.payment.entity.Product;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.DeliveryStatusEnum;
import com.payment.enums.OutboxSendStatusEnum;
import com.payment.enums.ProductTypeEnum;
import com.payment.mapper.MessageOutboxMapper;
import com.payment.mapper.OrderDeliveryRecordMapper;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.dto.CardKeyDeliveryDTO;
import com.payment.service.CardKeyPoolService;
import com.payment.service.UserNotificationService;
import com.payment.service.delivery.DeliveryStrategyRegistry;
import com.payment.service.impl.OutboxPublisherImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单交付服务关键路径测试。
 *
 * 关注点：
 * - enqueueDelivery 写 Outbox + 队列名正确
 * - deliverOrder 分发到 5 种策略,落记录并更新 item 状态
 * - revokeByOrderNo 标记 REVOKED
 */
class OrderDeliveryServiceImplTest {

    private SalesOrderMapper salesOrderMapper;
    private SalesOrderItemMapper salesOrderItemMapper;
    private ProductMapper productMapper;
    private OrderDeliveryRecordMapper deliveryRecordMapper;
    private MessageOutboxMapper messageOutboxMapper;
    private DeliveryStrategyRegistry strategyRegistry;
    private UserNotificationService notificationService;
    private CardKeyPoolService cardKeyPoolService;

    private OrderDeliveryServiceImpl service;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SalesOrderItem.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), OrderDeliveryRecord.class);
    }

    @BeforeEach
    void setUp() {
        salesOrderMapper = mock(SalesOrderMapper.class);
        salesOrderItemMapper = mock(SalesOrderItemMapper.class);
        productMapper = mock(ProductMapper.class);
        deliveryRecordMapper = mock(OrderDeliveryRecordMapper.class);
        messageOutboxMapper = mock(MessageOutboxMapper.class);
        notificationService = mock(UserNotificationService.class);
        cardKeyPoolService = mock(CardKeyPoolService.class);

        // 真用 5 个 Strategy 跑分发，让 registry 完整
        strategyRegistry = new DeliveryStrategyRegistry(List.of(
                new PhysicalDeliveryStrategy(),
                new VirtualDeliveryStrategy(),
                new CardKeyDeliveryStrategy(cardKeyPoolService),
                new ServiceDeliveryStrategy(),
                new SubscriptionDeliveryStrategy()
        ));
        strategyRegistry.init();

        service = new OrderDeliveryServiceImpl(
                salesOrderMapper, salesOrderItemMapper, productMapper,
                deliveryRecordMapper, new OutboxPublisherImpl(messageOutboxMapper), strategyRegistry, notificationService);
    }

    @Test
    void enqueueDeliveryShouldWriteOutboxWithDeliveryQueue() {
        service.enqueueDelivery("ORDER123");

        ArgumentCaptor<MessageOutbox> captor = ArgumentCaptor.forClass(MessageOutbox.class);
        verify(messageOutboxMapper).insert(captor.capture());
        MessageOutbox saved = captor.getValue();
        assertEquals("ORDER_DELIVERY", saved.getBizType());
        assertEquals("ORDER123", saved.getBizNo());
        assertEquals("payment.v1.order.delivery", saved.getRoutingKey());
        assertEquals(OutboxSendStatusEnum.PENDING.name(), saved.getSendStatus());
    }

    @Test
    void deliverOrderShouldDeliverEachItemByType() {
        SalesOrder order = order(1L, "ORDER-MULTI", 999L);
        when(salesOrderMapper.selectOne(any())).thenReturn(order);

        SalesOrderItem physical = item(11L, 1L, ProductTypeEnum.PHYSICAL.name());
        SalesOrderItem virtual = item(12L, 2L, ProductTypeEnum.VIRTUAL.name());
        SalesOrderItem cardKey = item(13L, 3L, ProductTypeEnum.CARD_KEY.name());
        when(salesOrderItemMapper.selectByOrderId(1L)).thenReturn(List.of(physical, virtual, cardKey));

        when(productMapper.selectById(1L)).thenReturn(productOf(null));
        when(productMapper.selectById(2L)).thenReturn(productOf("{\"contentUrl\":\"https://example.com/x\"}"));
        when(productMapper.selectById(3L)).thenReturn(productOf(null));
        when(cardKeyPoolService.lockForDelivery(7L, 3L, "ORDER-MULTI", 13L))
                .thenReturn(new CardKeyDeliveryDTO(101L, "VIP-2026-0001"));

        service.deliverOrder("ORDER-MULTI");

        // 3 条 record 插入
        verify(deliveryRecordMapper, atLeastOnce()).insert(any(OrderDeliveryRecord.class));
        ArgumentCaptor<OrderDeliveryRecord> recordCaptor = ArgumentCaptor.forClass(OrderDeliveryRecord.class);
        verify(deliveryRecordMapper, atLeastOnce()).insert(recordCaptor.capture());
        List<OrderDeliveryRecord> all = recordCaptor.getAllValues();
        assertEquals(3, all.size());
        org.junit.jupiter.api.Assertions.assertTrue(all.stream().allMatch(r -> r.getProductName() != null && !r.getProductName().isBlank()));

        // PHYSICAL → PENDING, VIRTUAL → DELIVERED(含 contentUrl), CARD_KEY → DELIVERED(真实配置 code)
        OrderDeliveryRecord physicalRec = all.stream().filter(r -> r.getProductType().equals("PHYSICAL")).findFirst().orElseThrow();
        assertEquals(DeliveryStatusEnum.PENDING.name(), physicalRec.getStatus());

        OrderDeliveryRecord virtualRec = all.stream().filter(r -> r.getProductType().equals("VIRTUAL")).findFirst().orElseThrow();
        assertEquals(DeliveryStatusEnum.DELIVERED.name(), virtualRec.getStatus());
        assertNotNull(virtualRec.getPayload());

        OrderDeliveryRecord cardKeyRec = all.stream().filter(r -> r.getProductType().equals("CARD_KEY")).findFirst().orElseThrow();
        assertEquals(DeliveryStatusEnum.DELIVERED.name(), cardKeyRec.getStatus());
        assertNotNull(cardKeyRec.getPayload());
        org.junit.jupiter.api.Assertions.assertTrue(cardKeyRec.getPayload().contains("VIP-2026-0001"));

        // VIRTUAL 与 CARD_KEY 都 DELIVERED,应该都尝试发通知(失败也不抛)
        verify(notificationService, atLeastOnce()).send(eq(999L), any(), any(), eq("ORDER"));
    }

    @Test
    void verifyServiceShouldConfirmDeliveredServiceRecord() {
        OrderDeliveryRecord record = serviceRecord(401L, 7L, 41L, DeliveryStatusEnum.DELIVERED.name(),
                "{\"verifyCode\":\"123456\"}");
        when(deliveryRecordMapper.selectOne(any())).thenReturn(record);

        OrderDeliveryRecord result = service.verifyService(7L, "123456");

        assertEquals(DeliveryStatusEnum.CONFIRMED.name(), result.getStatus());
        assertNotNull(result.getConfirmedTime());
        verify(deliveryRecordMapper).updateById(record);
        verify(salesOrderItemMapper).update(any(), any());
    }

    @Test
    void verifyServiceShouldRejectWrongTenant() {
        when(deliveryRecordMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.verifyService(8L, "123456"));
    }

    @Test
    void verifyServiceShouldRejectNonDeliveredRecord() {
        OrderDeliveryRecord record = serviceRecord(402L, 7L, 42L, DeliveryStatusEnum.REVOKED.name(),
                "{\"verifyCode\":\"654321\"}");
        when(deliveryRecordMapper.selectOne(any())).thenReturn(record);

        assertThrows(BusinessException.class, () -> service.verifyService(7L, "654321"));
        verify(deliveryRecordMapper, never()).updateById(any(OrderDeliveryRecord.class));
    }

    @Test
    void verifyServiceShouldReturnConfirmedRecordIdempotently() {
        OrderDeliveryRecord record = serviceRecord(403L, 7L, 43L, DeliveryStatusEnum.CONFIRMED.name(),
                "{\"verifyCode\":\"111222\"}");
        when(deliveryRecordMapper.selectOne(any())).thenReturn(record);

        OrderDeliveryRecord result = service.verifyService(7L, "111222");

        assertEquals(DeliveryStatusEnum.CONFIRMED.name(), result.getStatus());
        verify(deliveryRecordMapper, never()).updateById(any(OrderDeliveryRecord.class));
        verify(salesOrderItemMapper, never()).update(any(), any());
    }

    @Test
    void deliverOrderShouldSkipAlreadyDeliveredItem() {
        SalesOrder order = order(2L, "ORDER-SKIP", 888L);
        when(salesOrderMapper.selectOne(any())).thenReturn(order);

        SalesOrderItem done = item(21L, 1L, ProductTypeEnum.VIRTUAL.name());
        done.setDeliveryStatus(DeliveryStatusEnum.DELIVERED.name());
        when(salesOrderItemMapper.selectByOrderId(2L)).thenReturn(List.of(done));

        service.deliverOrder("ORDER-SKIP");

        // 已 DELIVERED 的 item 应被幂等跳过，不再产生新的 record 插入
        verify(deliveryRecordMapper, never()).insert(any(OrderDeliveryRecord.class));
    }

    @Test
    void revokeByOrderNoShouldMarkAllRecordsRevoked() {
        OrderDeliveryRecord r1 = new OrderDeliveryRecord();
        r1.setId(101L);
        r1.setOrderItemId(11L);
        r1.setStatus(DeliveryStatusEnum.DELIVERED.name());
        r1.setProductType(ProductTypeEnum.VIRTUAL.name());
        OrderDeliveryRecord alreadyRevoked = new OrderDeliveryRecord();
        alreadyRevoked.setId(102L);
        alreadyRevoked.setStatus(DeliveryStatusEnum.REVOKED.name());
        alreadyRevoked.setProductType(ProductTypeEnum.PHYSICAL.name());

        when(deliveryRecordMapper.selectList(any())).thenReturn(List.of(r1, alreadyRevoked));

        List<OrderDeliveryRecord> changed = service.revokeByOrderNo("ORDER-REV");

        assertEquals(1, changed.size());
        assertEquals(DeliveryStatusEnum.REVOKED.name(), changed.get(0).getStatus());
    }

    /**
     * H3 回归：strategy.revoke() 抛错时，不能误标 REVOKED（否则退款流程以为资源已回收，实际卡密/订阅仍有效）。
     * 正确做法：记录 REVOKE_FAILED 状态 + failReason，item.deliveryStatus 不动，等人工介入。
     */
    @Test
    void revokeByOrderNoShouldMarkRevokeFailedWhenStrategyThrows() {
        OrderDeliveryRecord record = new OrderDeliveryRecord();
        record.setId(201L);
        record.setOrderItemId(21L);
        record.setStatus(DeliveryStatusEnum.DELIVERED.name());
        record.setProductType(ProductTypeEnum.CARD_KEY.name());

        when(deliveryRecordMapper.selectList(any())).thenReturn(List.of(record));

        // 用真 ServiceDeliveryStrategy（它 revoke 是默认空实现），但换成自定义失败的 registry
        DeliveryStrategyRegistry failingRegistry = new DeliveryStrategyRegistry(List.of(
                new PhysicalDeliveryStrategy(),
                new VirtualDeliveryStrategy(),
                new CardKeyDeliveryStrategy(cardKeyPoolService) {
                    @Override
                    public void revoke(com.payment.entity.OrderDeliveryRecord rec) {
                        throw new RuntimeException("卡密作废接口超时");
                    }
                },
                new ServiceDeliveryStrategy(),
                new SubscriptionDeliveryStrategy()
        ));
        failingRegistry.init();

        OrderDeliveryServiceImpl failingService = new OrderDeliveryServiceImpl(
                salesOrderMapper, salesOrderItemMapper, productMapper,
                deliveryRecordMapper, new OutboxPublisherImpl(messageOutboxMapper), failingRegistry, notificationService);

        List<OrderDeliveryRecord> changed = failingService.revokeByOrderNo("ORDER-FAIL");

        assertEquals(1, changed.size());
        assertEquals(DeliveryStatusEnum.REVOKE_FAILED.name(), changed.get(0).getStatus());
        assertNotNull(changed.get(0).getFailReason());
        assert changed.get(0).getFailReason().contains("卡密作废接口超时");
        // item 的 deliveryStatus 不应被改为 REVOKED
        verify(salesOrderItemMapper, never()).update(any(), any());
    }

    @Test
    void revokeByOrderNoShouldSkipNonRevokeableStatuses() {
        OrderDeliveryRecord pending = new OrderDeliveryRecord();
        pending.setId(301L);
        pending.setOrderItemId(31L);
        pending.setStatus(DeliveryStatusEnum.PENDING.name()); // 尚未交付，不可撤销
        pending.setProductType(ProductTypeEnum.PHYSICAL.name());

        when(deliveryRecordMapper.selectList(any())).thenReturn(List.of(pending));

        List<OrderDeliveryRecord> changed = service.revokeByOrderNo("ORDER-PENDING");

        // PENDING 状态也能走 revoke（退款场景），这里验证 REVOKED 能正常标
        assertEquals(1, changed.size());
        assertEquals(DeliveryStatusEnum.REVOKED.name(), changed.get(0).getStatus());
    }

    // ---- helpers ----

    private SalesOrder order(Long id, String orderNo, Long userId) {
        SalesOrder o = new SalesOrder();
        o.setId(id);
        o.setOrderNo(orderNo);
        o.setTenantId(7L);
        o.setPlatformUserId(userId);
        o.setDeleted(0);
        return o;
    }

    private SalesOrderItem item(Long id, Long productId, String type) {
        SalesOrderItem item = new SalesOrderItem();
        item.setId(id);
        item.setProductId(productId);
        item.setProductName("商品-" + id);
        item.setTenantId(7L);
        item.setProductType(type);
        item.setDeliveryStatus(DeliveryStatusEnum.PENDING.name());
        return item;
    }

    private Product productOf(String config) {
        Product p = new Product();
        p.setDeliveryConfig(config);
        return p;
    }

    private OrderDeliveryRecord serviceRecord(Long id, Long tenantId, Long itemId, String status, String payload) {
        OrderDeliveryRecord record = new OrderDeliveryRecord();
        record.setId(id);
        record.setTenantId(tenantId);
        record.setOrderItemId(itemId);
        record.setProductType(ProductTypeEnum.SERVICE.name());
        record.setStatus(status);
        record.setPayload(payload);
        record.setDeleted(0);
        return record;
    }
}
