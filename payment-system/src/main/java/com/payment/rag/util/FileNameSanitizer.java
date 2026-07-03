package com.payment.rag.util;

import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 文件名安全清洗工具，防止路径穿越和非法文件名。
 */
public final class FileNameSanitizer {

    private FileNameSanitizer() {}

    /** 最大文件名长度（含扩展名） */
    private static final int MAX_FILENAME_LENGTH = 255;

    /** 控制字符（含 DEL 0x7F） */
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x1f\\x7f]");

    /** 路径分隔符 */
    private static final Pattern SEPARATORS = Pattern.compile("[/\\\\]");

    /** Windows 保留设备名（不含扩展名也匹配） */
    private static final Set<String> WINDOWS_RESERVED = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    );

    /**
     * 清洗用户提交的文件名，返回安全的纯文件名。
     * <p>
     * 处理策略：
     * 1. 去除前后空白
     * 2. 去除路径分隔符后的目录部分，仅保留最后的文件名
     * 3. 替换控制字符为空字符串
     * 4. 替换 ".." 片段为下划线
     * 5. 将 Windows 保留设备名替换为下划线前缀
     * 6. 截断超长文件名
     * 7. 若结果为空则使用 "unnamed" 作为兜底
     *
     * @param rawFilename 原始文件名
     * @return 安全文件名（纯名称，不含路径）
     */
    public static String sanitize(String rawFilename) {
        if (rawFilename == null || rawFilename.isBlank()) {
            return "unnamed";
        }

        String name = rawFilename.trim();

        // 1) 仅保留最后的路径分隔符之后的部分
        name = keepBaseName(name);

        // 2) 去除控制字符
        name = CONTROL_CHARS.matcher(name).replaceAll("");

        // 3) 去除路径穿越片段
        name = name.replace("..", "_");

        // 4) 去除路径分隔符残留（防御性）
        name = SEPARATORS.matcher(name).replaceAll("_");

        // 5) 处理 Windows 保留设备名（不区分大小写）
        name = replaceWindowsReserved(name);

        // 6) 截断超长文件名
        if (name.length() > MAX_FILENAME_LENGTH) {
            name = name.substring(0, MAX_FILENAME_LENGTH);
        }

        // 7) 兜底
        if (name.isBlank()) {
            return "unnamed";
        }

        return name;
    }

    /**
     * 校验合并后的目标路径是否仍在允许的目录下，防止写入路径穿越。
     *
     * @param resolvedPath 合并后 resolve 得到的绝对路径
     * @param allowedBase  允许的基准目录（绝对路径）
     * @return true 表示安全
     */
    public static boolean isInsideAllowedBase(Path resolvedPath, Path allowedBase) {
        try {
            Path normalizedBase = allowedBase.toAbsolutePath().normalize();
            Path normalizedTarget = resolvedPath.toAbsolutePath().normalize();
            return normalizedTarget.startsWith(normalizedBase);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 构建用于 MinIO 的安全 object key，避免路径穿越。
     * 格式：{prefix}/{sanitizedName}
     */
    public static String buildSafeObjectKey(String prefix, String rawFilename) {
        String safeName = sanitize(rawFilename);
        return prefix + "/" + safeName;
    }

    private static String keepBaseName(String name) {
        // 取最后一个路径分隔符（正斜杠或反斜杠）之后的部分
        int idx = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (idx >= 0 && idx < name.length() - 1) {
            return name.substring(idx + 1);
        }
        return name;
    }

    private static String replaceWindowsReserved(String name) {
        // 去掉扩展名后判断基名是否为保留名
        String upper = name.toUpperCase();
        for (String reserved : WINDOWS_RESERVED) {
            if (upper.equals(reserved) || upper.startsWith(reserved + ".")) {
                return "_" + name;
            }
        }
        return name;
    }
}
