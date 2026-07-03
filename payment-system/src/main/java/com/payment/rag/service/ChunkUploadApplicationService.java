package com.payment.rag.service;

import com.payment.rag.util.FileNameSanitizer;
import com.payment.rag.util.HashUtils;
import com.payment.rag.model.dto.ApiResponse;
import com.payment.rag.model.dto.ChunkUploadResponse;
import com.payment.rag.model.dto.ChunkUploadStatus;
import com.payment.rag.model.dto.FileExistenceResponse;
import com.payment.rag.model.dto.MergeChunkResult;
import com.payment.rag.model.dto.UploadChunkResult;
import com.payment.rag.model.dto.UploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class ChunkUploadApplicationService {

    private static final long MAX_CHUNK_SIZE_BYTES = 100L * 1024 * 1024;
    private static final long MAX_TOTAL_UPLOAD_SIZE_BYTES = 2L * 1024 * 1024 * 1024;

    private final ChunkUploadService chunkUploadService;
    private final RagUnitService ragUnitService;

    public ApiResponse<ChunkUploadResponse> checkUploadStatus(String fileHash,
                                                              String userId,
                                                              String filename,
                                                              Long fileSize,
                                                              Integer totalChunks) {
        if (!isValidSha256(fileHash)) {
            return ApiResponse.validationError("无效的 SHA-256 哈希值");
        }
        if (fileSize != null && fileSize > MAX_TOTAL_UPLOAD_SIZE_BYTES) {
            return ApiResponse.validationError("文件总大小超过限制（最大 2GB）");
        }

        try {
            FileExistenceResponse existenceCheck = ragUnitService.checkFileExists(userId, fileHash);
            if (existenceCheck.getExists()) {
                return ApiResponse.success("文件已存在，秒传成功",
                        ChunkUploadResponse.instantUpload(existenceCheck.getSourceId()));
            }
            if (existenceCheck.getStatus() != null) {
                return ApiResponse.success("文件已有处理记录",
                        ChunkUploadResponse.trackedFile(
                                existenceCheck.getSourceId(),
                                existenceCheck.getStatus(),
                                existenceCheck.getErrorMessage()
                        ));
            }

            ChunkUploadStatus status = chunkUploadService.checkUploadStatus(userId, fileHash, filename, fileSize, totalChunks);
            ChunkUploadResponse response = ChunkUploadResponse.needUpload(
                    status.getUploadedChunks() != null ? status.getUploadedChunks() : new HashSet<>(),
                    status.getProgress()
            );
            return ApiResponse.success("可以继续上传", response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ApiResponse<UploadChunkResult> uploadChunk(String fileHash,
                                                      String userId,
                                                      Integer chunkNumber,
                                                      MultipartFile chunk) {
        if (!isValidSha256(fileHash)) {
            return ApiResponse.validationError("无效的 SHA-256 哈希值");
        }
        if (chunkNumber == null || chunkNumber < 0) {
            return ApiResponse.validationError("无效的分片编号");
        }
        if (chunk == null || chunk.isEmpty()) {
            return ApiResponse.validationError("分片文件不能为空");
        }
        if (chunk.getSize() > MAX_CHUNK_SIZE_BYTES) {
            return ApiResponse.validationError("分片大小超过限制（最大 100MB）");
        }

        try {
            chunkUploadService.uploadChunk(userId, fileHash, chunkNumber, chunk);
            return ApiResponse.success("分片上传成功", UploadChunkResult.builder()
                    .fileHash(fileHash)
                    .chunkNumber(chunkNumber)
                    .success(true)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ApiResponse<MergeChunkResult> mergeChunks(String fileHash, String userId, String filename) {
        if (!isValidSha256(fileHash)) {
            return ApiResponse.validationError("无效的 SHA-256 哈希值");
        }
        if (filename == null || filename.trim().isEmpty()) {
            return ApiResponse.validationError("文件名不能为空");
        }

        try {
            String safeFilename = FileNameSanitizer.sanitize(filename);
            UploadResponse response = chunkUploadService.mergeChunks(userId, fileHash, safeFilename);
            return ApiResponse.success("合并成功", MergeChunkResult.builder()
                    .fileHash(response.getFileHash() != null ? response.getFileHash() : fileHash)
                    .filename(safeFilename)
                    .sourceId(response.getSourceId())
                    .status(response.getStatus() != null ? response.getStatus().name() : null)
                    .success(response.isSuccess())
                    .message(response.getMessage())
                    .errorMessage(response.getErrorMessage())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isValidSha256(String fileHash) {
        return HashUtils.isValidSha256(fileHash);
    }
}
