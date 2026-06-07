package com.payment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VO 转换工具类，统一 toFen / formatTime 公共方法。
 */
public final class VoConverterUtil {

    private VoConverterUtil() {
    }

    public static Long toFen(BigDecimal yuan) {
        return yuan == null ? null : yuan.multiply(BigDecimal.valueOf(100)).longValue();
    }

    public static String formatTime(LocalDateTime time) {
        return time == null ? null : time.toString();
    }
}
