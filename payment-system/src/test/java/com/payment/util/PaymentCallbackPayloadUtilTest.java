package com.payment.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCallbackPayloadUtilTest {

    @Test
    void missingSignedRequestIdShouldUseStablePayloadDigest() {
        String rawBody = "{\"out_trade_no\":\"PB-100\"}";

        String first = PaymentCallbackPayloadUtil.idempotencyKey("ALIPAY_PAGE", "PB-100", null, rawBody);
        String second = PaymentCallbackPayloadUtil.idempotencyKey("ALIPAY_PAGE", "PB-100", " ", rawBody);

        assertThat(first).isEqualTo(second).startsWith("CALLBACK-").hasSize(73);
    }

    @Test
    void signedRequestIdShouldTakePrecedenceWhenBounded() {
        String first = PaymentCallbackPayloadUtil.idempotencyKey(
                "ALIPAY_PAGE", "PB-100", "notify-100", "body");
        String second = PaymentCallbackPayloadUtil.idempotencyKey(
                "ALIPAY_PAGE", "PB-200", "notify-100", "body");

        assertThat(first).startsWith("CALLBACK-").hasSize(73);
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void payloadLimitShouldCountUtf8Bytes() {
        String rawBody = "中".repeat(22_000);

        assertThat(rawBody.length()).isLessThan(PaymentCallbackPayloadUtil.MAX_PAYLOAD_BYTES);
        assertThat(rawBody.getBytes(StandardCharsets.UTF_8).length)
                .isGreaterThan(PaymentCallbackPayloadUtil.MAX_PAYLOAD_BYTES);
        assertThat(PaymentCallbackPayloadUtil.isWithinLimit(rawBody)).isFalse();
    }
}
