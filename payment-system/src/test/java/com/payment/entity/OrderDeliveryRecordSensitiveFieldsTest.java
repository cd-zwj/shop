package com.payment.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderDeliveryRecordSensitiveFieldsTest {

    @Test
    void generatedObjectMethodsShouldExcludeSensitivePickupFields() {
        OrderDeliveryRecord first = new OrderDeliveryRecord();
        first.setId(1L);
        first.setPayload("sensitive-payload");
        first.setPickupCodeHash("sensitive-hash");
        OrderDeliveryRecord second = new OrderDeliveryRecord();
        second.setId(1L);
        second.setPayload("other-payload");
        second.setPickupCodeHash("other-hash");

        assertThat(first.toString())
                .doesNotContain("sensitive-payload", "sensitive-hash", "payload", "pickupCodeHash");
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }
}
