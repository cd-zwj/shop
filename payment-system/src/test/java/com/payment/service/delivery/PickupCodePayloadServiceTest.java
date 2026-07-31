package com.payment.service.delivery;

import com.payment.entity.OrderDeliveryRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PickupCodePayloadServiceTest {

    private static final String KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    private final PickupCodePayloadService payloadService = new PickupCodePayloadService(
            new PickupCodeCryptoService("v1", "v1=" + KEY));

    @Test
    void encryptedPayloadShouldNotContainPlaintextAndShouldRoundTrip() {
        String payload = payloadService.createEncryptedPayload(9L, "SO001", 11L, 7L, "12345678");
        OrderDeliveryRecord record = record(payload);

        assertThat(payload).contains("pickupCodeCiphertext").doesNotContain("12345678");
        assertThat(payloadService.readPickupCode(record)).isEqualTo("12345678");
        assertThat(payloadService.requiresRotation(record)).isFalse();
    }

    @Test
    void legacyPlaintextPayloadShouldBeReadableAndRotatedWithoutPlaintext() {
        OrderDeliveryRecord record = record("{\"pickupCode\":\"12345678\",\"storeId\":7}");

        assertThat(payloadService.readPickupCode(record)).isEqualTo("12345678");
        assertThat(payloadService.requiresRotation(record)).isTrue();
        assertThat(payloadService.rotatePayload(record))
                .contains("pickupCodeCiphertext")
                .doesNotContain("12345678");
    }

    @Test
    void presentButInvalidCiphertextShouldNeverDowngradeToLegacyPlaintext() {
        OrderDeliveryRecord nullCiphertext = record(
                "{\"pickupCodeCiphertext\":null,\"pickupCode\":\"12345678\"}");
        OrderDeliveryRecord blankCiphertext = record(
                "{\"pickupCodeCiphertext\":\"\",\"pickupCode\":\"12345678\"}");

        assertThatThrownBy(() -> payloadService.readPickupCode(nullCiphertext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("取货凭证内容无效");
        assertThatThrownBy(() -> payloadService.readPickupCode(blankCiphertext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("取货凭证内容无效");
    }

    @Test
    void malformedActiveKeyCiphertextShouldFailRotationValidation() {
        OrderDeliveryRecord record = record(
                "{\"pickupCodeCiphertext\":\"pc1.v1.invalid.invalid\"}");

        assertThatThrownBy(() -> payloadService.requiresRotation(record))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("取货码密文校验失败");
    }

    private OrderDeliveryRecord record(String payload) {
        OrderDeliveryRecord record = new OrderDeliveryRecord();
        record.setTenantId(9L);
        record.setOrderNo("SO001");
        record.setOrderItemId(11L);
        record.setStoreId(7L);
        record.setPayload(payload);
        return record;
    }
}
