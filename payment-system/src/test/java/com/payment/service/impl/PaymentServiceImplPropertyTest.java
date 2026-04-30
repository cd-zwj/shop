package com.payment.service.impl;

import com.payment.config.PaymentConfig;
import com.payment.dto.PayResponseDTO;
import com.payment.entity.PaymentOrder;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 支付服务属性测试。
 */
public class PaymentServiceImplPropertyTest {

    private PaymentConfig paymentConfig;
    private NativePayService nativePayService;
    private PaymentServiceImpl paymentService;

    @Property(tries = 20)
    @Label("Feature: backend-todo-completion, Property 1: 微信支付订单创建正确性")
    void wechatPaymentCreationShouldReturnValidQRCode(@ForAll("validPaymentOrders") PaymentOrder order) throws Exception {
        initializePaymentService();

        String expectedQrCodeUrl = "weixin://wxpay/bizpayurl?pr=" + order.getOrderNo();
        PrepayResponse prepayResponse = new PrepayResponse();
        prepayResponse.setCodeUrl(expectedQrCodeUrl);
        when(nativePayService.prepay(any(PrepayRequest.class))).thenReturn(prepayResponse);

        PayResponseDTO response = paymentService.createPay(order);

        assertNotNull(response, "支付响应不应为空");
        assertEquals(order.getOrderNo(), response.getOrderNo(), "订单号应匹配");
        assertEquals("WECHAT", response.getPayType(), "支付类型应为 WECHAT");
        assertEquals(order.getAmount(), response.getAmount(), "金额应匹配");
        assertNotNull(response.getQrCode(), "二维码链接不应为空");
        assertTrue(response.getQrCode().startsWith("weixin://"), "二维码链接应以 weixin:// 开头");
        assertNotNull(response.getPayUrl(), "支付链接不应为空");
        assertTrue(response.getPayUrl().contains("weixin"), "支付链接应包含 weixin");
    }

    @Provide
    Arbitrary<PaymentOrder> validPaymentOrders() {
        return Combinators.combine(
                Arbitraries.strings().alpha().numeric().ofMinLength(16).ofMaxLength(32),
                Arbitraries.bigDecimals().between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(10000.00)).ofScale(2),
                Arbitraries.strings().alpha().numeric().withChars(' ').ofMinLength(1).ofMaxLength(50),
                Arbitraries.strings().alpha().numeric().withChars(' ', ',', '.').ofMinLength(0).ofMaxLength(200)
        ).as((orderNo, amount, subject, body) -> {
            PaymentOrder order = new PaymentOrder();
            order.setId(1L);
            order.setTenantId(1L);
            order.setUserId(1L);
            order.setOrderNo(orderNo);
            order.setAmount(amount);
            order.setPayAmount(amount);
            order.setSubject(subject);
            order.setBody(body);
            order.setPayType("WECHAT");
            order.setOrderStatus("PENDING");
            order.setNotifyUrl("https://test.example.com/notify");
            order.setCreateTime(LocalDateTime.now());
            order.setExpireTime(LocalDateTime.now().plusHours(2));
            order.setDeleted(0);
            return order;
        });
    }

    private void initializePaymentService() throws Exception {
        MockitoAnnotations.openMocks(this);

        paymentConfig = mock(PaymentConfig.class);
        nativePayService = mock(NativePayService.class);

        PaymentConfig.Wechat wechat = new PaymentConfig.Wechat();
        wechat.setAppId("wx1234567890abcdef");
        wechat.setMchId("1234567890");
        wechat.setApiV3Key("test-api-v3-key-must-be-32-chars");
        wechat.setKeyPath("classpath:cert/wechat/test_key.pem");
        wechat.setCertPath("classpath:cert/wechat/test_cert.pem");
        wechat.setNotifyUrl("https://test.example.com/api/payment/wechat/notify");
        when(paymentConfig.getWechat()).thenReturn(wechat);

        paymentService = new PaymentServiceImpl();
        injectField("paymentConfig", paymentConfig);
        injectField("wechatConfig", mock(Config.class));
        injectField("nativePayService", nativePayService);
    }

    private void injectField(String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = PaymentServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(paymentService, value);
    }
}
