package com.payment.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.payment.rag.mapper.RagUnitMapper;
import com.payment.rag.model.DocumentFile;
import com.payment.rag.model.RagUnit;
import com.payment.rag.model.dto.DeleteTaskStatus;
import com.payment.rag.model.dto.FileDeleteTask;
import com.payment.rag.util.HashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentDeleteService {

    private final RagUnitMapper ragUnitMapper;
    private final FileDeleteProducer fileDeleteProducer;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DocumentFileService documentFileService;

    private static final String DELETE_TASK_PREFIX = "delete:task:";

    @Transactional
    public String asyncDeleteDocument(String userId, String fileHash) {
        validateFileHash(fileHash);

        DocumentFile documentFile = documentFileService.getActiveByFileHash(userId, fileHash);
        if (documentFile == null) {
            throw new RuntimeException("文档不存在: " + fileHash);
        }

        List<RagUnit> targetUnits = findUnitsBySourceId(documentFile.getSourceId());
        List<String> unitIds = new ArrayList<>();
        List<String> vectorIds = new ArrayList<>();

        for (RagUnit unit : targetUnits) {
            unitIds.add(unit.getId());
            vectorIds.add(unit.getId());
        }

        FileDeleteTask task = FileDeleteTask.builder()
                .taskId(UUID.randomUUID().toString())
                .filename(documentFile.getFilename())
                .fileHash(fileHash)
                .userId(userId)
                .minioPath(documentFile.getMinioPath())
                .vectorIds(vectorIds)
                .unitIds(unitIds)
                .createTimestamp(System.currentTimeMillis())
                .build();

        try {
            documentFileService.markDeleted(userId, fileHash);
            saveTaskStatus(task);
            fileDeleteProducer.sendDeleteTask(task);
            return task.getTaskId();
        } catch (Exception e) {
            documentFileService.restoreVisible(userId, fileHash);
            throw e;
        }
    }

    @Deprecated
    public String asyncDeleteDocumentByFilename(String userId, String filename) {
        DocumentFile documentFile = documentFileService.getActiveByFilename(userId, filename);
        if (documentFile == null) {
            throw new RuntimeException("文档不存在: " + filename);
        }
        return asyncDeleteDocument(userId, documentFile.getFileHash());
    }

    public DeleteTaskStatus getDeleteStatus(String taskId) {
        Object obj = redisTemplate.opsForValue().get(DELETE_TASK_PREFIX + taskId);
        if (obj == null) {
            throw new RuntimeException("任务不存在或已过期: " + taskId);
        }
        return DeleteTaskStatus.fromTask((FileDeleteTask) obj);
    }

    @Deprecated
    public void safeDeleteDocumentSync(String filename) {
        throw new UnsupportedOperationException("请使用异步删除接口");
    }

    public List<String> asyncBatchDelete(List<String> identifiers) {
        List<String> taskIds = new ArrayList<>();
        for (String identifier : identifiers) {
            try {
                if (identifier != null && HashUtils.isValidSha256(identifier)) {
                    taskIds.add(asyncDeleteDocument(null, identifier));
                } else {
                    taskIds.add(asyncDeleteDocumentByFilename(null, identifier));
                }
            } catch (Exception e) {
                log.error("提交删除任务失败: {}", identifier, e);
            }
        }
        return taskIds;
    }

    private List<RagUnit> findUnitsBySourceId(String sourceId) {
        QueryWrapper<RagUnit> wrapper = new QueryWrapper<>();
        wrapper.eq("source_id", sourceId);
        return ragUnitMapper.selectList(wrapper);
    }

    private void saveTaskStatus(FileDeleteTask task) {
        redisTemplate.opsForValue().set(DELETE_TASK_PREFIX + task.getTaskId(), task, 24, TimeUnit.HOURS);
    }

    private void validateFileHash(String fileHash) {
        if (fileHash == null || fileHash.trim().isEmpty()) {
            throw new IllegalArgumentException("文件哈希值不能为空");
        }
        if (!HashUtils.isValidSha256(fileHash)) {
            throw new IllegalArgumentException("无效的 SHA-256 哈希格式");
        }
    }
}
