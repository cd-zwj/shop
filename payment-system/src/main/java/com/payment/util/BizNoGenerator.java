package com.payment.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 业务编号生成工具类。
 * <p>
 * 生成格式为：{前缀} + yyyyMMddHHmmss + 8位随机UUID片段（大写）。
 * 例如：ORD20240615143025A1B2C3D4
 * </p>
 */
public final class BizNoGenerator {

    /** 日期时间格式化器，精确到秒 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 私有构造，禁止实例化工具类 */
    private BizNoGenerator() {
    }

    /**
     * 生成业务编号。
     *
     * @param prefix 业务前缀，如 "ORD"（订单）、"PAY"（支付）、"REF"（退款）
     * @return 唯一业务编号字符串
     */
    public static String generate(String prefix) {
        return prefix + FORMATTER.format(LocalDateTime.now()) + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
