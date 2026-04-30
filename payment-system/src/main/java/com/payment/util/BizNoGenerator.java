package com.payment.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class BizNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private BizNoGenerator() {
    }

    public static String generate(String prefix) {
        return prefix + FORMATTER.format(LocalDateTime.now()) + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
