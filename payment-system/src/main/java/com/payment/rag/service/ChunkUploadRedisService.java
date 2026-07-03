package com.payment.rag.service;

import com.payment.rag.model.dto.ChunkUploadStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 分片上传Redis管理服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChunkUploadRedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${file.upload.expire-hours:24}")
    private int expireHours;

    private static final String CHUNK_UPLOAD_PREFIX = "chunk:upload:";
    private static final String CHUNK_SET_PREFIX = "chunk:set:";  // 新增：用于存储已上传分片的 SET

    /**
     * 获取Redis Key
     */
    private String getKey(String userId, String fileHash) {
        return CHUNK_UPLOAD_PREFIX + userId + ":" + fileHash;
    }

    /**
     * 获取已上传分片 SET 的 Key
     */
    private String getChunkSetKey(String userId, String fileHash) {
        return CHUNK_SET_PREFIX + userId + ":" + fileHash;
    }

    /**
     * 初始化上传状态
     */
    public void initUploadStatus(String userId, String fileHash, String filename, Long fileSize, Integer totalChunks, String tempDir) {
        ChunkUploadStatus status = ChunkUploadStatus.builder()
                .fileHash(fileHash)
                .filename(filename)
                .fileSize(fileSize)
                .totalChunks(totalChunks)
                .uploadedChunks(new HashSet<>())
                .status(ChunkUploadStatus.UploadStatus.UPLOADING)
                .tempDir(tempDir)
                .createTime(System.currentTimeMillis())
                .updateTime(System.currentTimeMillis())
                .build();

        String key = getKey(userId, fileHash);
        redisTemplate.opsForValue().set(key, status, expireHours, TimeUnit.HOURS);
        log.info("初始化上传状态: userId={}, fileHash={}, totalChunks={}", userId, fileHash, totalChunks);
    }

    /**
     * 获取上传状态
     */
    public ChunkUploadStatus getUploadStatus(String userId, String fileHash) {
        String key = getKey(userId, fileHash);
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) {
            return null;
        }

        ChunkUploadStatus status = (ChunkUploadStatus) obj;
        Set<Integer> uploadedChunks = getUploadedChunks(userId, fileHash);
        status.setUploadedChunks(uploadedChunks);
        if (status.getTotalChunks() != null
                && uploadedChunks.size() >= status.getTotalChunks()
                && status.getStatus() == ChunkUploadStatus.UploadStatus.UPLOADING) {
            status.setStatus(ChunkUploadStatus.UploadStatus.COMPLETED);
        }
        return status;
    }

    /**
     * 标记分片已上传（使用 Redis SET 解决并发竞态问题）
     */
    public void markChunkUploaded(String userId, String fileHash, Integer chunkNumber) {
        String key = getKey(userId, fileHash);
        String chunkSetKey = getChunkSetKey(userId, fileHash);

        // 1. 使用 Redis SET 原子性添加已上传分片（解决竞态条件）
        redisTemplate.opsForSet().add(chunkSetKey, chunkNumber);
        redisTemplate.expire(chunkSetKey, expireHours, TimeUnit.HOURS);

        // 2. 获取当前状态
        ChunkUploadStatus status = getUploadStatus(userId, fileHash);
        if (status == null) {
            log.warn("上传状态不存在: userId={}, fileHash={}", userId, fileHash);
            return;
        }

        // 3. 从 Redis SET 获取所有已上传分片数量
        Long uploadedCount = redisTemplate.opsForSet().size(chunkSetKey);
        status.setUploadedChunks(getUploadedChunks(userId, fileHash));
        status.setUpdateTime(System.currentTimeMillis());

        // 4. 检查是否所有分片都已上传
        if (uploadedCount != null && uploadedCount >= status.getTotalChunks()) {
            status.setStatus(ChunkUploadStatus.UploadStatus.COMPLETED);
            log.info("所有分片上传完成: userId={}, fileHash={}, totalChunks={}", userId, fileHash, uploadedCount);
        }

        redisTemplate.opsForValue().set(key, status, expireHours, TimeUnit.HOURS);
        log.debug("标记分片已上传: userId={}, fileHash={}, chunk={}, uploaded={}/{}",
                userId, fileHash, chunkNumber, uploadedCount, status.getTotalChunks());
    }

    /**
     * 删除上传状态
     */
    public void deleteUploadStatus(String userId, String fileHash) {
        String key = getKey(userId, fileHash);
        String chunkSetKey = getChunkSetKey(userId, fileHash);

        // 删除状态对象和分片 SET
        redisTemplate.delete(key);
        redisTemplate.delete(chunkSetKey);
        log.info("删除上传状态: userId={}, fileHash={}", userId, fileHash);
    }

    /**
     * 检查分片是否已上传（从 Redis SET 中检查）
     */
    public boolean isChunkUploaded(String userId, String fileHash, Integer chunkNumber) {
        String chunkSetKey = getChunkSetKey(userId, fileHash);
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(chunkSetKey, chunkNumber));
    }

    /**
     * 标记上传失败
     */
    public void markUploadFailed(String userId, String fileHash) {
        ChunkUploadStatus status = getUploadStatus(userId, fileHash);
        if (status != null) {
            status.setStatus(ChunkUploadStatus.UploadStatus.FAILED);
            status.setUpdateTime(System.currentTimeMillis());
            String key = getKey(userId, fileHash);
            redisTemplate.opsForValue().set(key, status, expireHours, TimeUnit.HOURS);
            log.error("标记上传失败: userId={}, fileHash={}", userId, fileHash);
        }
    }

    private Set<Integer> getUploadedChunks(String userId, String fileHash) {
        String chunkSetKey = getChunkSetKey(userId, fileHash);
        Set<Object> members = redisTemplate.opsForSet().members(chunkSetKey);
        if (members == null || members.isEmpty()) {
            return new HashSet<>();
        }

        Set<Integer> uploadedChunks = new HashSet<>();
        for (Object member : members) {
            Integer chunkNumber = toInteger(member);
            if (chunkNumber != null) {
                uploadedChunks.add(chunkNumber);
            }
        }
        return uploadedChunks;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Integer integerValue) {
            return integerValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException ex) {
                log.warn("无法将 Redis 分片编号转换为整数: {}", stringValue);
                return null;
            }
        }
        log.warn("忽略无法识别的 Redis 分片编号类型: {}", value != null ? value.getClass().getName() : "null");
        return null;
    }
}
