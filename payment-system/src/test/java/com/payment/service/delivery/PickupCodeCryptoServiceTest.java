package com.payment.service.delivery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PickupCodeCryptoServiceTest {

    private static final String KEY_V1 = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    private static final String KEY_V2 = "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=";

    @Test
    void encryptShouldUseRandomNonceAndBindCiphertextToOrderContext() {
        PickupCodeCryptoService crypto = new PickupCodeCryptoService("v1", "v1=" + KEY_V1);

        String first = crypto.encrypt(9L, "SO001", 11L, "12345678");
        String second = crypto.encrypt(9L, "SO001", 11L, "12345678");

        assertThat(first).startsWith("pc1.v1.").doesNotContain("12345678");
        assertThat(second).isNotEqualTo(first);
        assertThat(crypto.decrypt(9L, "SO001", 11L, first)).isEqualTo("12345678");
        assertThatThrownBy(() -> crypto.decrypt(9L, "SO002", 11L, first))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("取货码密文校验失败");
    }

    @Test
    void rotatedKeyringShouldReadOldCiphertextAndWriteWithActiveKey() {
        PickupCodeCryptoService oldCrypto = new PickupCodeCryptoService("v1", "v1=" + KEY_V1);
        String oldCiphertext = oldCrypto.encrypt(9L, "SO001", 11L, "12345678");
        PickupCodeCryptoService rotated = new PickupCodeCryptoService(
                "v2", "v1=" + KEY_V1 + ",v2=" + KEY_V2);

        assertThat(rotated.decrypt(9L, "SO001", 11L, oldCiphertext)).isEqualTo("12345678");
        assertThat(rotated.requiresRotation(oldCiphertext)).isTrue();
        assertThat(rotated.encrypt(9L, "SO001", 11L, "87654321")).startsWith("pc1.v2.");
    }

    @Test
    void constructorShouldRejectMissingUnknownOrWeakKeys() {
        assertThatThrownBy(() -> new PickupCodeCryptoService("", ""))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PickupCodeCryptoService("v2", "v1=" + KEY_V1))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PickupCodeCryptoService("v1", "v1=YWJj"))
                .isInstanceOf(IllegalStateException.class);
    }
}
