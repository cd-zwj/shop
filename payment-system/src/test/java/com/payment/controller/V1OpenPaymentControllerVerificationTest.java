package com.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.common.GlobalExceptionHandler;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.service.PaymentBillV1Service;
import com.payment.service.PaymentSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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

    @BeforeEach
    void setUp() {
        V1OpenPaymentController controller = new V1OpenPaymentController(paymentBillV1Service, signatureVerifier);
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
    }
}
