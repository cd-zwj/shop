package com.payment.service.impl;

import com.payment.config.PaymentConfig;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.mapper.PaymentBillMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentSignatureVerifierImpl - 支付回调验签")
class PaymentSignatureVerifierImplTest {

    @Mock
    private PaymentBillMapper paymentBillMapper;

    @Test
    @DisplayName("未知渠道必须拒绝")
    void unknownChannelShouldBeRejected() {
        PaymentSignatureVerifierImpl verifier = new PaymentSignatureVerifierImpl(buildConfig(), paymentBillMapper);
        PaymentCallbackDTO dto = buildCallback("{\"billNo\":\"PB-001\"}");

        boolean valid = verifier.verify("UNKNOWN", dto, Map.of());

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("缺少原始报文时必须拒绝 JSON 回调")
    void jsonCallbackWithoutRawBodyShouldBeRejected() {
        PaymentSignatureVerifierImpl verifier = new PaymentSignatureVerifierImpl(buildConfig(), paymentBillMapper);
        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB-001");

        boolean valid = verifier.verify("ALIPAY_PAGE", dto, Map.of("sign", "invalid"));

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("支付宝回调缺少 sign 必须拒绝")
    void alipayCallbackWithoutSignShouldBeRejected() {
        PaymentSignatureVerifierImpl verifier = new PaymentSignatureVerifierImpl(buildConfig(), paymentBillMapper);

        boolean valid = verifier.verifyAlipayCallback(Map.of(
                "out_trade_no", "PB-001",
                "trade_no", "202606050001",
                "trade_status", "TRADE_SUCCESS",
                "app_id", "test-app"
        ));

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("支付宝回调签名无效必须拒绝")
    void alipayCallbackWithInvalidSignShouldBeRejected() {
        PaymentSignatureVerifierImpl verifier = new PaymentSignatureVerifierImpl(buildConfig(), paymentBillMapper);

        boolean valid = verifier.verifyAlipayCallback(Map.of(
                "out_trade_no", "PB-001",
                "trade_no", "202606050001",
                "trade_status", "TRADE_SUCCESS",
                "app_id", "test-app",
                "sign_type", "RSA2",
                "sign", "invalid-sign"
        ));

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("第三方渠道缺少签名必须拒绝")
    void extProviderWithoutSignatureShouldBeRejected() {
        PaymentSignatureVerifierImpl verifier = new PaymentSignatureVerifierImpl(buildConfig(), paymentBillMapper);
        PaymentCallbackDTO dto = buildCallback("{\"billNo\":\"PB-EXT\",\"success\":true}");

        boolean valid = verifier.verify("EXT_PROVIDER", dto, Map.of());

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("非成功状态不应映射为成功")
    void nonSuccessTradeStatusShouldNotBeTreatedAsSuccess() {
        PaymentSignatureVerifierImpl verifier = new PaymentSignatureVerifierImpl(buildConfig(), paymentBillMapper);

        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "PB-100");
        params.put("trade_no", "trade-100");
        params.put("trade_status", "WAIT_BUYER_PAY");
        params.put("app_id", "test-app");
        params.put("sign_type", "RSA2");
        params.put("sign", "signature-1");

        assertThat(verifier.verifyTradeSuccess(params)).isFalse();
    }

    private PaymentCallbackDTO buildCallback(String rawBody) {
        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo("PB-001");
        dto.setCallbackRequestId("notify-1");
        dto.setThirdPartyBillNo("trade-1");
        dto.setSuccess(true);
        dto.setRawBody(rawBody);
        return dto;
    }

    private PaymentConfig buildConfig() {
        PaymentConfig config = new PaymentConfig();

        PaymentConfig.Alipay alipay = new PaymentConfig.Alipay();
        alipay.setAppId("test-app");
        alipay.setPublicKey("invalid-public-key");
        config.setAlipay(alipay);

        PaymentConfig.ExtProvider extProvider = new PaymentConfig.ExtProvider();
        extProvider.setMerchantKey("test-secret");
        config.setExtProvider(extProvider);

        PaymentConfig.Wechat wechat = new PaymentConfig.Wechat();
        wechat.setMchId("test-mch");
        wechat.setApiV3Key("test-api-v3-key");
        wechat.setKeyPath("/not/exist.pem");
        wechat.setMerchantSerialNumber("serial-no");
        config.setWechat(wechat);

        return config;
    }
}
