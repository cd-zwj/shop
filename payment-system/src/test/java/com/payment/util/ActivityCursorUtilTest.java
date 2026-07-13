package com.payment.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityCursorUtilTest {

    @Test
    void decodeShouldAcceptKnownActivitySource() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 7, 13, 12, 30);
        String cursor = ActivityCursorUtil.encode(occurredAt, "MEMBER_POINTS_LOG", 42L);

        ActivityCursorUtil.DecodedCursor decoded = ActivityCursorUtil.decode(cursor);

        assertThat(decoded).isNotNull();
        assertThat(decoded.occurredAt()).isEqualTo(occurredAt);
        assertThat(decoded.sourceType()).isEqualTo("MEMBER_POINTS_LOG");
        assertThat(decoded.sourceId()).isEqualTo(42L);
    }

    @Test
    void decodeShouldRejectUnknownActivitySource() {
        String cursor = ActivityCursorUtil.encode(
                LocalDateTime.of(2026, 7, 13, 12, 30),
                "ARBITRARY_SOURCE",
                42L);

        assertThat(ActivityCursorUtil.decode(cursor)).isNull();
    }
}
