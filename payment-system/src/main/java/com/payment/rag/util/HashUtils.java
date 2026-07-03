package com.payment.rag.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * SHA-256 哈希计算工具类，支持流式读取和文件读取。
 */
public final class HashUtils {

    private static final int BUFFER_SIZE = 8192;

    private static final Pattern SHA256_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");

    private HashUtils() {
    }

    /**
     * 校验字符串是否为合法的 SHA-256 十六进制格式（64 位 hex）。
     *
     * @param hash 待校验字符串
     * @return true 表示格式合法
     */
    public static boolean isValidSha256(String hash) {
        return hash != null && SHA256_PATTERN.matcher(hash).matches();
    }

    /**
     * 流式读取 InputStream 并计算 SHA-256。
     * <p>
     * 调用方负责关闭传入的流；此方法会读到流结束。
     *
     * @param is 输入流
     * @return SHA-256 十六进制字符串（小写，64 字符）
     * @throws IOException 读取流失败
     */
    public static String hashInputStream(InputStream is) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in all Java implementations
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }

    /**
     * 读取文件并计算 SHA-256。
     *
     * @param path 文件路径
     * @return SHA-256 十六进制字符串（小写，64 字符）
     * @throws IOException 读取文件失败
     */
    public static String hashFile(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return hashInputStream(is);
        }
    }

    /**
     * 包装传入的 InputStream，使其在读取过程中同步计算 SHA-256。
     * <p>
     * 关闭返回的 DigestInputStream 后，通过 {@link DigestInputStream#getMessageDigest()} 获取结果。
     *
     * @param is 原始输入流
     * @return DigestInputStream 包装流
     */
    public static DigestInputStream wrapWithDigest(InputStream is) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return new DigestInputStream(is, digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }

    /**
     * 从 DigestInputStream 提取已完成计算的 hex 字符串。
     *
     * @param dis 已读完的 DigestInputStream
     * @return SHA-256 十六进制字符串（小写，64 字符）
     */
    public static String extractHex(DigestInputStream dis) {
        return bytesToHex(dis.getMessageDigest().digest());
    }

    /**
     * 计算任意字符串的 SHA-256（主要用于测试）。
     *
     * @param input 输入字符串
     * @return SHA-256 十六进制字符串（小写，64 字符）
     */
    public static String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}
