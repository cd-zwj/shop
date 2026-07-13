package com.payment.util;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * 资产动态游标编解码工具。
 *
 * 游标内部格式：occurredAt|sourceType|sourceId
 * 编码为 Base64URL 字符串，供前端透传。
 */
public final class ActivityCursorUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String SEPARATOR = "|";
    private static final Set<String> SOURCE_TYPES = Set.of(
            "UNIFIED_WALLET_LOG",
            "MERCHANT_WALLET_LOG",
            "MEMBER_POINTS_LOG",
            "MEMBER_GROWTH_LOG",
            "COUPON_RECEIVE_RECORD",
            "COUPON_LOCK_RECORD",
            "COUPON_RELEASE_RECORD",
            "COUPON_WRITE_OFF_RECORD",
            "COUPON_EXPIRE_RECORD");

    private ActivityCursorUtil() {
    }

    /**
     * 编码游标。
     *
     * @param occurredAt 发生时间
     * @param sourceType 来源类型
     * @param sourceId   来源ID
     * @return Base64URL 编码的游标字符串
     */
    public static String encode(LocalDateTime occurredAt, String sourceType, Long sourceId) {
        if (occurredAt == null || !SOURCE_TYPES.contains(sourceType) || sourceId == null || sourceId <= 0) {
            return null;
        }
        String raw = occurredAt.format(FORMATTER) + SEPARATOR + sourceType + SEPARATOR + sourceId;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解码游标。
     *
     * @param cursor Base64URL 编码的游标字符串
     * @return 包含 occurredAt、sourceType、sourceId 的数组；解析失败返回 null
     */
    public static DecodedCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", 3);
            if (parts.length != 3) {
                return null;
            }
            LocalDateTime occurredAt = LocalDateTime.parse(parts[0], FORMATTER);
            String sourceType = parts[1];
            Long sourceId = Long.parseLong(parts[2]);
            if (!SOURCE_TYPES.contains(sourceType) || sourceId <= 0) {
                return null;
            }
            return new DecodedCursor(occurredAt, sourceType, sourceId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 校验 types 参数是否合法。
     *
     * @param types 资产类型列表
     * @return 合法返回 true
     */
    public static boolean isValidTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return true;
        }
        return types.stream().allMatch(t ->
                "WALLET".equals(t) || "POINTS".equals(t) || "GROWTH".equals(t) || "COUPON".equals(t));
    }

    public record DecodedCursor(LocalDateTime occurredAt, String sourceType, Long sourceId) {
    }
}
