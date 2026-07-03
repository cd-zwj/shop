package com.payment.rag.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * 字符编码工具类，提供自适应的字节数组→字符串转换。
 */
public final class CharsetUtils {

    private CharsetUtils() {
    }

    /**
     * 将字节数组解码为字符串，自动处理编码回退。
     * <p>
     * 策略：严格 UTF-8 → GB18030 → 宽容 UTF-8（丢弃无法解码的字节）
     *
     * @param bytes 原始字节
     * @return 解码后的字符串
     */
    public static String readStringWithFallback(byte[] bytes) {
        // 1) 严格 UTF-8：遇到非法字节序列立即报错
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            CharBuffer buffer = decoder.decode(ByteBuffer.wrap(bytes));
            return buffer.toString();
        } catch (Exception ignored) {
        }

        // 2) GB18030：兼容中文 Windows 系统导出的文件
        try {
            return new String(bytes, Charset.forName("GB18030"));
        } catch (Exception ignored) {
        }

        // 3) 宽容 UTF-8：丢弃无法解码的字节
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
