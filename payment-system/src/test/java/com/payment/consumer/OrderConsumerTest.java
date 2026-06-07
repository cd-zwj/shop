package com.payment.consumer;

import com.payment.entity.PaymentOrder;
import com.payment.service.CouponService;
import com.payment.service.MemberService;
import com.payment.service.MessageIdempotentService;
import com.payment.service.PaymentOrderService;
import com.payment.service.PointsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderConsumerTest {

    @Mock
    private MessageIdempotentService messageIdempotentService;

    @Mock
    private PaymentOrderService paymentOrderService;

    @Mock
    private PointsService pointsService;

    @Mock
    private MemberService memberService;

    @Mock
    private CouponService couponService;

    private OrderConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderConsumer();
        // 注入 @Autowired 字段
        setField(consumer, "messageIdempotentService", messageIdempotentService);
        setField(consumer, "paymentOrderService", paymentOrderService);
        setField(consumer, "pointsService", pointsService);
        setField(consumer, "memberService", memberService);
        setField(consumer, "couponService", couponService);
    }

    @Test
    void testProcessOrderPaid_正常发放积分并升级会员() {
        // Arrange
        PaymentOrder order = paidOrder("SO1001", 9L, 100L, new BigDecimal("200.00"));
        when(paymentOrderService.getOrderByNo("SO1001")).thenReturn(order);
        when(pointsService.calculatePoints(new BigDecimal("200.00"), 9L)).thenReturn(20);

        // Act - 通过 processOrderPaid 反射调用
        invokeProcessOrderPaid("SO1001", messageMap("msg-001", "SO1001"));

        // Assert
        verify(pointsService).grantPoints(100L, 20, "订单支付", "SO1001");
        verify(memberService).checkAndAutoUpgrade(9L, 100L);
    }

    @Test
    void testProcessOrderPaid_积分发放失败不影响会员升级() {
        // Arrange
        PaymentOrder order = paidOrder("SO1001", 9L, 100L, new BigDecimal("200.00"));
        when(paymentOrderService.getOrderByNo("SO1001")).thenReturn(order);
        when(pointsService.calculatePoints(new BigDecimal("200.00"), 9L)).thenReturn(20);
        // 积分发放抛异常
        org.mockito.Mockito.doThrow(new RuntimeException("积分系统不可用"))
                .when(pointsService).grantPoints(100L, 20, "订单支付", "SO1001");

        // Act
        invokeProcessOrderPaid("SO1001", messageMap("msg-001", "SO1001"));

        // Assert - 会员升级仍然被执行（积分失败被 catch）
        verify(memberService).checkAndAutoUpgrade(9L, 100L);
    }

    @Test
    void testProcessOrderPaid_充值订单跳过处理() {
        // Arrange - 充值订单号以 "R" 开头
        PaymentOrder order = paidOrder("R202606070001", 9L, 100L, new BigDecimal("500.00"));
        when(paymentOrderService.getOrderByNo("R202606070001")).thenReturn(order);

        // Act
        invokeProcessOrderPaid("R202606070001", messageMap("msg-002", "R202606070001"));

        // Assert - 不发放积分，不检查会员升级
        verify(pointsService, never()).calculatePoints(any(), anyLong());
        verify(pointsService, never()).grantPoints(anyLong(), anyInt(), anyString(), anyString());
        verify(memberService, never()).checkAndAutoUpgrade(anyLong(), anyLong());
    }

    @Test
    void testProcessOrderCreated_订单不存在跳过() {
        // Arrange
        when(paymentOrderService.getOrderByNo("SO9999")).thenReturn(null);

        // Act
        invokeProcessOrderCreated("SO9999", messageMap("msg-003", "SO9999"));

        // Assert - 无任何后续操作
        verify(pointsService, never()).grantPoints(anyLong(), anyInt(), anyString(), anyString());
    }

    // ---- helper methods ----

    private PaymentOrder paidOrder(String orderNo, Long tenantId, Long userId, BigDecimal amount) {
        PaymentOrder order = new PaymentOrder();
        order.setId(1L);
        order.setOrderNo(orderNo);
        order.setTenantId(tenantId);
        order.setUserId(userId);
        order.setAmount(amount);
        order.setOrderStatus("PAID");
        return order;
    }

    private Map<String, Object> messageMap(String messageId, String orderNo) {
        Map<String, Object> map = new HashMap<>();
        map.put("messageId", messageId);
        map.put("orderNo", orderNo);
        return map;
    }

    /**
     * 通过反射调用 private processOrderPaid 方法
     */
    private void invokeProcessOrderPaid(String orderNo, Map<String, Object> messageMap) {
        try {
            var method = OrderConsumer.class.getDeclaredMethod("processOrderPaid", String.class, Map.class);
            method.setAccessible(true);
            method.invoke(consumer, orderNo, messageMap);
        } catch (Exception e) {
            // unwrap InvocationTargetException
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * 通过反射调用 private processOrderCreated 方法
     */
    private void invokeProcessOrderCreated(String orderNo, Map<String, Object> messageMap) {
        try {
            var method = OrderConsumer.class.getDeclaredMethod("processOrderCreated", String.class, Map.class);
            method.setAccessible(true);
            method.invoke(consumer, orderNo, messageMap);
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * 通过反射设置 @Autowired 字段
     */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = OrderConsumer.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
