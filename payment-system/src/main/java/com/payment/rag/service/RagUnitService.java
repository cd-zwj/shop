package com.payment.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.payment.rag.mapper.RagUnitMapper;
import com.payment.rag.util.FileNameSanitizer;
import com.payment.rag.model.DocumentFile;
import com.payment.rag.model.DocumentFileStatus;
import com.payment.rag.model.RagNodeType;
import com.payment.rag.model.RagUnit;
import com.payment.rag.model.SourceType;
import com.payment.rag.model.dto.DocumentFileStatusResponse;
import com.payment.rag.model.dto.FileExistenceResponse;
import com.payment.rag.model.dto.FileProcessTask;
import com.payment.rag.model.dto.PageRequest;
import com.payment.rag.model.dto.PageResponse;
import com.payment.rag.model.dto.RagDocumentInfo;
import com.payment.rag.model.dto.UploadResponse;
import com.payment.rag.service.processor.MediaProcessor;
import com.payment.rag.service.processor.MediaProcessorRegistry;
import com.payment.rag.util.HashUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.apache.tika.Tika;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class RagUnitService {

    private static final int DB_BATCH_FLUSH_SIZE = 500;

    // Allowed MIME types for file upload
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain", "text/markdown", "text/x-markdown", "text/x-web-markdown", "text/html", "text/csv",
            "application/json", "application/xml", "application/yaml", "application/x-yaml",
            "image/png", "image/jpeg", "image/gif", "image/webp", "image/bmp",
            "video/mp4", "video/webm", "video/quicktime",
            "application/mp4"
    );

    private final RagUnitMapper ragUnitMapper;
    private final SqlSessionFactory sqlSessionFactory;
    private final UploadService uploadService;
    private final VectorStoreWriteService vectorStoreWriteService;
    private final MediaProcessorRegistry processorRegistry;
    private final FileProcessProducer fileProcessProducer;
    private final DocumentFileService documentFileService;
    private final DocumentDeleteService documentDeleteService;
    private final HierarchicalIndexingService hierarchicalIndexingService;
    private final Tika tika = new Tika();

    public RagUnitService(RagUnitMapper ragUnitMapper,
                          SqlSessionFactory sqlSessionFactory,
                          UploadService uploadService,
                          VectorStoreWriteService vectorStoreWriteService,
                          MediaProcessorRegistry processorRegistry,
                          FileProcessProducer fileProcessProducer,
                          DocumentFileService documentFileService,
                          DocumentDeleteService documentDeleteService,
                          HierarchicalIndexingService hierarchicalIndexingService) {
        this.ragUnitMapper = ragUnitMapper;
        this.sqlSessionFactory = sqlSessionFactory;
        this.uploadService = uploadService;
        this.vectorStoreWriteService = vectorStoreWriteService;
        this.processorRegistry = processorRegistry;
        this.fileProcessProducer = fileProcessProducer;
        this.documentFileService = documentFileService;
        this.documentDeleteService = documentDeleteService;
        this.hierarchicalIndexingService = hierarchicalIndexingService;
    }

    /**
     * 使用 ExecutorType.BATCH 批量写入 rag_unit 表。
     * <p>
     * 注意：此方法依赖 Spring 托管的事务连接（SpringManagedTransactionFactory），
     * 由 mybatis-plus-spring-boot3-starter 默认配置。
     * 因此在事务模板（如 FileProcessConsumer.saveDataWithTransaction）中调用时，
     * 使用 SqlSessionUtils 确保 batch session 正确参与 Spring 事务，失败时可整体回滚。
     * createdAt / updatedAt 由 MyBatis-Plus MetaObjectHandler 在 flushStatements 时自动填充。
     */
    public void saveBatch(List<RagUnit> units) {
        if (units == null || units.isEmpty()) {
            return;
        }
        SqlSession sqlSession = SqlSessionUtils.getSqlSession(sqlSessionFactory, ExecutorType.BATCH, null);
        try {
            RagUnitMapper batchMapper = sqlSession.getMapper(RagUnitMapper.class);
            for (int i = 0; i < units.size(); i++) {
                batchMapper.insert(units.get(i));
                if ((i + 1) % DB_BATCH_FLUSH_SIZE == 0) {
                    sqlSession.flushStatements();
                }
            }
            sqlSession.flushStatements();
        } finally {
            SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
        }
        log.debug("批量写入 MySQL 完成, 记录数: {}", units.size());
    }

    public FileExistenceResponse checkFileExists(String userId, String fileHash) {
        validateFileHash(fileHash);
        validateUserId(userId);

        DocumentFile documentFile = documentFileService.getByFileHash(userId, fileHash);
        if (documentFile == null || Boolean.TRUE.equals(documentFile.getDeleted())) {
            return FileExistenceResponse.notExists();
        }

        List<RagUnit> units = getUnitsBySourceId(documentFile.getSourceId());
        List<RagUnit> leafUnits = filterLeafUnits(units);
        List<String> leafUnitIds = new ArrayList<>();
        for (RagUnit unit : leafUnits) {
            leafUnitIds.add(unit.getId());
        }

        if (documentFile.getStatus() == DocumentFileStatus.SUCCESS) {
            return FileExistenceResponse.exists(
                    documentFile.getSourceId(),
                    documentFile.getFilename(),
                    safeChunkCount(documentFile, leafUnits),
                    documentFile.getMinioPath(),
                    documentFile.getMinioUrl(),
                    leafUnitIds
            );
        }

        if (documentFile.getStatus() != null && documentFile.getStatus().isProcessing()) {
            return FileExistenceResponse.processing(
                    documentFile.getSourceId(),
                    documentFile.getFilename(),
                    safeChunkCount(documentFile, leafUnits),
                    documentFile.getMinioPath(),
                    documentFile.getMinioUrl(),
                    documentFile.getStatus()
            );
        }

        if (documentFile.getStatus() == DocumentFileStatus.FAILED) {
            return FileExistenceResponse.failed(
                    documentFile.getSourceId(),
                    documentFile.getFilename(),
                    safeChunkCount(documentFile, leafUnits),
                    documentFile.getMinioPath(),
                    documentFile.getMinioUrl(),
                    documentFile.getErrorMessage()
            );
        }

        return FileExistenceResponse.notExists();
    }

    public DocumentFileStatusResponse getDocumentStatus(String userId, String fileHash) {
        validateFileHash(fileHash);
        validateUserId(userId);
        return documentFileService.getDocumentStatus(userId, fileHash);
    }

    public UploadResponse processAndStoreAsync(MultipartFile file, String fileHash, String userId) throws Exception {
        String filename = file.getOriginalFilename();
        long fileSize = file.getSize();

        validateFileHash(fileHash);
        validateUserId(userId);

        // 先用客户端 hash 做快速去重（防御性检查）
        DocumentFile existing = documentFileService.getByFileHash(userId, fileHash);
        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getDeleted())) {
                throw new IllegalStateException("该文档正在删除中，请稍后重试");
            }
            if (existing.getStatus() == DocumentFileStatus.SUCCESS) {
                return buildUploadResponse(existing, "文件已存在，无需重复上传", true);
            }
            if (existing.getStatus() != null && existing.getStatus().isProcessing()) {
                return buildUploadResponse(existing, "文件已在后台处理中，请稍后查看状态", true);
            }
            if (existing.getStatus() == DocumentFileStatus.FAILED) {
                throw new IllegalStateException("该文档已有失败记录，请先删除后重新上传");
            }
        }

        // 服务端重算 SHA-256，顺带检测 mimeType
        String mimeType;
        String serverFileHash;
        try (DigestInputStream dis = HashUtils.wrapWithDigest(file.getInputStream())) {
            mimeType = tika.detect(dis, filename);
            validateMimeType(mimeType);
            serverFileHash = HashUtils.extractHex(dis);
        }

        // 用服务端重算的 hash 再做一次去重，防止客户端 hash 不一致的情况
        if (!serverFileHash.equals(fileHash)) {
            log.warn("客户端 hash 与服务端 hash 不一致: client={}, server={}, userId={}, filename={}",
                    fileHash, serverFileHash, userId, filename);
            DocumentFile serverExisting = documentFileService.getByFileHash(userId, serverFileHash);
            if (serverExisting != null) {
                if (Boolean.TRUE.equals(serverExisting.getDeleted())) {
                    throw new IllegalStateException("该文档正在删除中，请稍后重试");
                }
                if (serverExisting.getStatus() == DocumentFileStatus.SUCCESS) {
                    return buildUploadResponse(serverExisting, "文件已存在，无需重复上传", true);
                }
                if (serverExisting.getStatus() != null && serverExisting.getStatus().isProcessing()) {
                    return buildUploadResponse(serverExisting, "文件已在后台处理中，请稍后查看状态", true);
                }
                if (serverExisting.getStatus() == DocumentFileStatus.FAILED) {
                    throw new IllegalStateException("该文档已有失败记录，请先删除后重新上传");
                }
            }
        }

        MediaProcessor processor = findProcessorByMimeType(mimeType);
        if (processor == null) {
            throw new IllegalArgumentException("不支持的文件类型: " + mimeType);
        }

        SourceType sourceType = determineSourceType(mimeType);
        String safeFilename = FileNameSanitizer.sanitize(filename);
        String sourceId = UUID.randomUUID().toString();
        String minioPath = FileNameSanitizer.buildSafeObjectKey(sourceId, safeFilename);

        // 使用服务端重算的 hash 作为 fileHash 存入数据库，确保完整性校验可信
        documentFileService.createUploadingRecord(userId, sourceId, serverFileHash, filename, fileSize, sourceType);

        try {
            uploadService.uploadFile(file, minioPath);
            String minioUrl = uploadService.getFileUrl(minioPath);
            documentFileService.markUploadSuccess(userId, serverFileHash, sourceType, minioPath, minioUrl);

            FileProcessTask task = FileProcessTask.builder()
                    .sourceId(sourceId)
                    .filename(filename)
                    .fileHash(serverFileHash)
                    .userId(userId)
                    .mimeType(mimeType)
                    .minioPath(minioPath)
                    .minioUrl(minioUrl)
                    .fileSize(fileSize)
                    .createTimestamp(System.currentTimeMillis())
                    .build();

            fileProcessProducer.sendFileProcessTask(task);

            return buildUploadResponse(
                    documentFileService.getActiveByFileHash(userId, serverFileHash),
                    "文件上传成功，正在后台处理中...",
                    true
            );
        } catch (Exception e) {
            documentFileService.markFailed(userId, serverFileHash, normalizeErrorMessage(e));
            throw e;
        }
    }

    public MediaProcessor findProcessorByMimeType(String mimeType) {
        return processorRegistry.findByMimeType(mimeType);
    }

    public String deleteDocumentAsync(String userId, String fileHash) {
        validateUserId(userId);
        return documentDeleteService.asyncDeleteDocument(userId, fileHash);
    }

    @Deprecated
    public String deleteDocumentAsyncByFilename(String userId, String filename) {
        validateUserId(userId);
        return documentDeleteService.asyncDeleteDocumentByFilename(userId, filename);
    }

    @Deprecated
    public void deleteDocument(String filename) {
        documentDeleteService.safeDeleteDocumentSync(filename);
    }

    @Deprecated
    public void deleteDocumentUnsafe(String filename) {
        QueryWrapper<RagUnit> wrapper = new QueryWrapper<>();
        wrapper.like("minio_path", "/" + filename);

        List<RagUnit> units = ragUnitMapper.selectList(wrapper);
        List<RagUnit> targetUnits = new ArrayList<>();
        String targetPath = null;

        for (RagUnit unit : units) {
            if (unit.getMinioPath() != null && unit.getMinioPath().endsWith("/" + filename)) {
                targetUnits.add(unit);
                targetPath = unit.getMinioPath();
            }
        }

        if (targetUnits.isEmpty()) {
            throw new RuntimeException("Document not found: " + filename);
        }

        if (targetPath != null) {
            uploadService.deleteFile(targetPath);
        }

        List<String> idsToDelete = new ArrayList<>();
        for (RagUnit unit : targetUnits) {
            idsToDelete.add(unit.getId());
            ragUnitMapper.deleteById(unit.getId());
        }

        if (!idsToDelete.isEmpty()) {
            vectorStoreWriteService.deleteFromVectorStores(idsToDelete);
        }
    }

    public List<String> getAllDocuments(String userId) {
        PageRequest request = new PageRequest();
        request.setPageSize(1000);
        request.setUserId(userId);
        List<String> filenames = new ArrayList<>();
        for (RagDocumentInfo info : documentFileService.getDocumentsPage(request).getRecords()) {
            filenames.add(info.getFilename());
        }
        return filenames;
    }

    public PageResponse<RagDocumentInfo> getDocumentsPage(PageRequest request) {
        return documentFileService.getDocumentsPage(request);
    }

    public List<RagUnit> getUnitsBySourceId(String sourceId) {
        QueryWrapper<RagUnit> wrapper = new QueryWrapper<>();
        wrapper.eq("source_id", sourceId)
                .orderByAsc("tree_level")
                .orderByAsc("ordinal")
                .orderByAsc("chunk_index");
        return ragUnitMapper.selectList(wrapper);
    }

    public List<RagUnit> getLeafUnitsBySourceId(String sourceId) {
        return filterLeafUnits(getUnitsBySourceId(sourceId));
    }

    public void removeIndexedData(String sourceId) {
        removeIndexedData(sourceId, List.of());
    }

    public void removeIndexedData(String sourceId, List<String> fallbackUnitIds) {
        List<RagUnit> existingUnits = getUnitsBySourceId(sourceId);
        Set<String> ids = new LinkedHashSet<>();
        for (RagUnit unit : existingUnits) {
            ids.add(unit.getId());
        }
        if (fallbackUnitIds != null) {
            ids.addAll(fallbackUnitIds);
        }

        if (!ids.isEmpty()) {
            vectorStoreWriteService.deleteFromVectorStores(new ArrayList<>(ids));
        }

        QueryWrapper<RagUnit> wrapper = new QueryWrapper<>();
        wrapper.eq("source_id", sourceId);
        ragUnitMapper.delete(wrapper);
    }

    public void addUnitsToVectorStores(List<RagUnit> units, String filename) {
        vectorStoreWriteService.addUnitsToVectorStores(units, filename);
    }


    private UploadResponse buildUploadResponse(DocumentFile documentFile, String message, boolean success) {
        List<RagUnit> leafUnits = documentFile.getStatus() == DocumentFileStatus.SUCCESS
                ? getLeafUnitsBySourceId(documentFile.getSourceId())
                : List.of();
        List<String> unitIds = new ArrayList<>();
        for (RagUnit unit : leafUnits) {
            unitIds.add(unit.getId());
        }

        return UploadResponse.builder()
                .success(success)
                .message(message)
                .sourceId(documentFile.getSourceId())
                .fileHash(documentFile.getFileHash())
                .filename(documentFile.getFilename())
                .sourceType(documentFile.getSourceType())
                .status(documentFile.getStatus())
                .errorMessage(documentFile.getErrorMessage())
                .chunksCreated(safeChunkCount(documentFile, leafUnits))
                .minioPath(documentFile.getMinioPath())
                .minioUrl(documentFile.getMinioUrl())
                .unitIds(unitIds)
                .build();
    }

    private int safeChunkCount(DocumentFile documentFile, List<RagUnit> leafUnits) {
        if (documentFile.getChunkCount() != null && documentFile.getChunkCount() > 0) {
            return documentFile.getChunkCount();
        }
        return leafUnits.size();
    }

    private List<RagUnit> filterLeafUnits(List<RagUnit> units) {
        List<RagUnit> leafUnits = new ArrayList<>();
        for (RagUnit unit : units) {
            if (unit.getNodeType() == null || unit.getNodeType() == RagNodeType.LEAF) {
                leafUnits.add(unit);
            }
        }
        return leafUnits;
    }

    private void validateFileHash(String fileHash) {
        if (fileHash == null || fileHash.trim().isEmpty()) {
            throw new IllegalArgumentException("文件哈希值不能为空");
        }
        if (!fileHash.matches("^[a-fA-F0-9]{64}$")) {
            throw new IllegalArgumentException("无效的 SHA-256 哈希格式");
        }
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
    }

    private String normalizeErrorMessage(Exception e) {
        if (e == null || e.getMessage() == null || e.getMessage().isBlank()) {
            return "文件处理失败";
        }
        return e.getMessage();
    }

    private void validateMimeType(String mimeType) {
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("不支持的文件类型: " + mimeType);
        }
    }

    private SourceType determineSourceType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return SourceType.TEXT;
        }
        if (mimeType.startsWith("image/")) {
            return SourceType.IMAGE;
        }
        if (mimeType.startsWith("video/")) {
            return SourceType.VIDEO;
        }
        return SourceType.TEXT;
    }
}
