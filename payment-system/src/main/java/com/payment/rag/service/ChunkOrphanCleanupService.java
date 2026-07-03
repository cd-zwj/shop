package com.payment.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 分片上传孤儿清理服务。
 * <p>
 * 定时扫描临时目录，删除无对应 Redis 任务且超过保留时间的磁盘文件。
 * Redis Key 有 TTL 自动过期，但磁盘文件没有，需要此服务兜底。
 */
@Service
@Slf4j
public class ChunkOrphanCleanupService {

    private static final String CHUNK_UPLOAD_PREFIX = "chunk:upload:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${file.upload.temp-dir:./temp/chunks}")
    private String tempDirBase;

    /** 孤儿判定阈值（小时）：文件最后修改时间距今超过此值且无 Redis Key 则视为孤儿 */
    @Value("${file.upload.orphan-threshold-hours:24}")
    private int orphanThresholdHours;

    public ChunkOrphanCleanupService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 每天凌晨 3 点扫描并清理孤儿临时文件。
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOrphanedChunks() {
        Path baseDir = Paths.get(tempDirBase).toAbsolutePath().normalize();
        if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
            return;
        }

        Instant cutoff = Instant.now().minus(orphanThresholdHours, ChronoUnit.HOURS);
        int cleanedCount = 0;
        long cleanedBytes = 0;

        try (DirectoryStream<Path> userDirs = Files.newDirectoryStream(baseDir)) {
            for (Path userDir : userDirs) {
                if (!Files.isDirectory(userDir)) {
                    continue;
                }
                String userId = userDir.getFileName().toString();

                try (DirectoryStream<Path> hashDirs = Files.newDirectoryStream(userDir)) {
                    for (Path hashDir : hashDirs) {
                        if (!Files.isDirectory(hashDir)) {
                            continue;
                        }
                        String fileHash = hashDir.getFileName().toString();

                        // 检查最后修改时间
                        FileTime lastModified = Files.getLastModifiedTime(hashDir);
                        if (lastModified.toInstant().isAfter(cutoff)) {
                            continue; // 还在保留期内，跳过
                        }

                        // 检查 Redis 中是否有活跃任务
                        String redisKey = CHUNK_UPLOAD_PREFIX + userId + ":" + fileHash;
                        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                            continue; // 有活跃任务，跳过
                        }

                        // 无 Redis Key 且超过阈值，删除
                        long dirSize = calculateDirectorySize(hashDir);
                        deleteDirectory(hashDir);
                        cleanedCount++;
                        cleanedBytes += dirSize;
                        log.info("清理分片孤儿: userId={}, fileHash={}, size={}bytes", userId, fileHash, dirSize);
                    }

                    // 如果用户目录为空，也一并删除
                    if (isDirectoryEmpty(userDir)) {
                        Files.deleteIfExists(userDir);
                    }
                }
            }
        } catch (IOException e) {
            log.error("扫描孤儿临时文件时出错", e);
        }

        if (cleanedCount > 0) {
            log.info("分片孤儿清理完成: 删除 {} 个目录, 释放 {} KB", cleanedCount, cleanedBytes / 1024);
        } else {
            log.debug("分片孤儿清理完成: 无孤儿文件");
        }
    }

    private long calculateDirectorySize(Path dir) {
        long size = 0;
        try (Stream<Path> paths = Files.walk(dir)) {
            size = paths.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try { return Files.size(p); } catch (IOException e) { return 0; }
                    })
                    .sum();
        } catch (IOException e) {
            log.warn("计算目录大小失败: {}", dir, e);
        }
        return size;
    }

    private void deleteDirectory(Path dir) {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    log.warn("删除孤儿文件失败: {}", path, e);
                }
            });
        } catch (IOException e) {
            log.error("删除孤儿目录失败: {}", dir, e);
        }
    }

    private boolean isDirectoryEmpty(Path dir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            return !stream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }
}
