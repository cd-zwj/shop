package com.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.common.GlobalExceptionHandler;
import com.payment.common.BusinessException;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.enums.PaymentCallbackFailureReasonEnum;
import com.payment.service.PaymentBillV1Service;
import com.payment.service.PaymentCallbackAuditService;
import com.payment.service.PaymentSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("V1OpenPaymentController - 回调验签门控")
class V1OpenPaymentControllerVerificationTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PaymentBillV1Service paymentBillV1Service;

    @Mock
    private PaymentSignatureVerifier signatureVerifier;

    @Mock
    private PaymentCallbackAuditService callbackAuditService;

    @BeforeEach
    void setUp() {
        V1OpenPaymentController controller = new V1OpenPaymentController(
                paymentBillV1Service, signatureVerifier, callbackAuditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("未知渠道验签失败时不调用 handleCallback")
    void unknownChannelRejectedShouldNotCallHandleCallback() throws Exception {
        when(signatureVerifier.verify(eq("UNKNOWN"), any(PaymentCallbackDTO.class), any(Map.class)))
                .thenReturn(false);

        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB-001");
        dto.setRawBody("{\"billNo\":\"PB-001\"}");

        mockMvc.perform(post("/v1/open/payments/callbacks/{channelCode}", "UNKNOWN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("回调验签失败"));

        then(paymentBillV1Service).should(never()).handleCallback(any(), any());
        then(callbackAuditService).should().recordRejected(eq("UNKNOWN"), any(PaymentCallbackDTO.class),
                eq(PaymentCallbackFailureReasonEnum.SIGNATURE_INVALID));
    }

    @Test
    @DisplayName("验签失败时不调用 handleCallback")
    void failedVerificationShouldNotCallHandleCallback() throws Exception {
        when(signatureVerifier.verify(eq("ALIPAY_PAGE"), any(PaymentCallbackDTO.class), any(Map.class)))
                .thenReturn(false);

        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB-002");
        dto.setRawBody("{\"billNo\":\"PB-002\"}");

        mockMvc.perform(post("/v1/open/payments/callbacks/{channelCode}", "ALIPAY_PAGE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("回调验签失败"));

        then(paymentBillV1Service).should(never()).handleCallback(any(), any());
        then(callbackAuditService).should().recordRejected(eq("ALIPAY_PAGE"), any(PaymentCallbackDTO.class),
                eq(PaymentCallbackFailureReasonEnum.SIGNATURE_INVALID));
    }

    @Test
    @DisplayName("验签成功时必须调用 handleCallback")
    void successfulVerificationShouldCallHandleCallback() throws Exception {
        when(signatureVerifier.verify(eq("ALIPAY_PAGE"), any(PaymentCallbackDTO.class), any(Map.class)))
                .thenReturn(true);

        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB-003");
        dto.setRawBody("{\"billNo\":\"PB-003\"}");

        mockMvc.perform(post("/v1/open/payments/callbacks/{channelCode}", "ALIPAY_PAGE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));

        then(paymentBillV1Service).should().handleCallback(eq("ALIPAY_PAGE"), any(PaymentCallbackDTO.class));
        then(callbackAuditService).should(never()).recordRejected(any(), any(), any());
    }

    @Test
    @DisplayName("支付宝关闭交易应归一化为明确失败终态")
    void alipayTradeClosedShouldBeMappedToTerminalFailure() {
        when(signatureVerifier.verifyAlipayCallback(any())).thenReturn(true);
        V1OpenPaymentController controller = new V1OpenPaymentController(
                paymentBillV1Service, signatureVerifier, callbackAuditService);

        controller.handleAlipayPageCallback(Map.of(
                "out_trade_no", "PB-004",
                "trade_no", "ALI-004",
                "notify_id", "notify-004",
                "trade_status", "TRADE_CLOSED"
        ));

        ArgumentCaptor<PaymentCallbackDTO> captor = ArgumentCaptor.forClass(PaymentCallbackDTO.class);
        then(paymentBillV1Service).should().handleCallback(eq("ALIPAY_PAGE"), captor.capture());
        assertThat(captor.getValue().getSuccess()).isFalse();
        assertThat(captor.getValue().getTerminalFailure()).isTrue();
    }

    @Test
    @DisplayName("支付宝表单验签失败时写入拒绝审计")
    void failedAlipayFormVerificationShouldWriteRejectedAudit() {
        when(signatureVerifier.verifyAlipayCallback(any())).thenReturn(false);
        V1OpenPaymentController controller = new V1OpenPaymentController(
                paymentBillV1Service, signatureVerifier, callbackAuditService);
        Map<String, String> params = Map.of(
                "out_trade_no", "PB-005",
                "trade_no", "ALI-005",
                "notify_id", "notify-005",
                "trade_status", "TRADE_SUCCESS"
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                com.payment.common.BusinessException.class,
                () -> controller.handleAlipayPageCallback(params));

        then(callbackAuditService).should().recordRejected(
                eq("ALIPAY_PAGE"), any(PaymentCallbackDTO.class),
                eq(PaymentCallbackFailureReasonEnum.SIGNATURE_INVALID));
        then(paymentBillV1Service).should(never()).handleCallback(any(), any());
    }

    @Test
    @DisplayName("扩展渠道验签失败时写入拒绝审计")
    void failedExtProviderVerificationShouldWriteRejectedAudit() {
        when(signatureVerifier.verify(eq("EXT_PROVIDER"), any(PaymentCallbackDTO.class), any(Map.class)))
                .thenReturn(false);
        V1OpenPaymentController controller = new V1OpenPaymentController(
                paymentBillV1Service, signatureVerifier, callbackAuditService);
        Map<String, String> params = Map.of(
                "out_trade_no", "PB-006",
                "trade_no", "EXT-006",
                "trade_status", "TRADE_CLOSED"
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                com.payment.common.BusinessException.class,
                () -> controller.handleExtProviderCallback(params));

        then(callbackAuditService).should().recordRejected(
                eq("EXT_PROVIDER"), any(PaymentCallbackDTO.class),
                eq(PaymentCallbackFailureReasonEnum.SIGNATURE_INVALID));
        then(paymentBillV1Service).should(never()).handleCallback(any(), any());
    }

    @Test
    @DisplayName("超大回调报文应在验签和业务处理前拒绝")
    void oversizedPayloadShouldBeRejectedBeforeVerification() throws Exception {
        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB-007");
        dto.setCallbackRequestId("notify-007");
        dto.setRawBody("x".repeat(70_000));

        mockMvc.perform(post("/v1/open/payments/callbacks/{channelCode}", "ALIPAY_PAGE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("支付回调报文过大"));

        then(callbackAuditService).should().recordRejected(
                eq("ALIPAY_PAGE"), any(PaymentCallbackDTO.class),
                eq(PaymentCallbackFailureReasonEnum.PAYLOAD_INVALID));
        then(signatureVerifier).should(never()).verify(any(), any(), any());
        then(paymentBillV1Service).should(never()).handleCallback(any(), any());
    }

    @Test
    @DisplayName("验签器异常时应记录系统验签错误并继续拒绝")
    void verifierExceptionShouldWriteErrorAuditAndReject() throws Exception {
        when(signatureVerifier.verify(eq("ALIPAY_PAGE"), any(PaymentCallbackDTO.class), any(Map.class)))
                .thenThrow(new BusinessException("certificate unavailable"));
        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB-008");
        dto.setRawBody("{\"out_trade_no\":\"PB-008\"}");

        mockMvc.perform(post("/v1/open/payments/callbacks/{channelCode}", "ALIPAY_PAGE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("回调验签失败"));

        then(callbackAuditService).should().recordRejected(
                eq("ALIPAY_PAGE"), any(PaymentCallbackDTO.class),
                eq(PaymentCallbackFailureReasonEnum.SIGNATURE_VERIFICATION_ERROR));
        then(paymentBillV1Service).should(never()).handleCallback(any(), any());
    }
}
