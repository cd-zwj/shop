package com.payment.rag.service;

import com.payment.rag.model.dto.ApiResponse;
import com.payment.rag.model.dto.FileExistenceResponse;
import com.payment.rag.model.dto.UploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UploadApplicationService {

    private static final int MAX_BATCH_UPLOAD_FILES = 20;

    private final RagUnitService ragUnitService;

    public ApiResponse<FileExistenceResponse> checkFileExists(String fileHash, String userId) {
        try {
            FileExistenceResponse response = ragUnitService.checkFileExists(userId, fileHash);
            if (response.getExists()) {
                return ApiResponse.success("文件已存在", response);
            }
            if (response.getStatus() != null) {
                return ApiResponse.success("文件已有处理记录", response);
            }
            return ApiResponse.success("文件不存在，可以上传", response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.validationError(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ApiResponse<UploadResponse> uploadFile(MultipartFile file, String fileHash, String userId) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.validationError("文件不能为空");
        }
        if (fileHash == null || fileHash.trim().isEmpty()) {
            return ApiResponse.validationError("文件哈希值不能为空");
        }

        try {
            UploadResponse response = ragUnitService.processAndStoreAsync(file, fileHash, userId);
            return ApiResponse.success(response.getMessage(), response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ApiResponse<List<UploadResponse>> uploadFiles(MultipartFile[] files, String[] fileHashes, String userId) {
        ApiResponse<List<UploadResponse>> validationError = validateBatchUpload(files, fileHashes);
        if (validationError != null) {
            return validationError;
        }

        try {
            List<UploadResponse> responses = new ArrayList<>();
            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;

            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                String fileHash = fileHashes[i];

                if (file == null || file.isEmpty()) {
                    responses.add(buildFailedUploadResponse(file, fileHash, "文件不能为空"));
                    failCount++;
                    continue;
                }

                try {
                    UploadResponse response = ragUnitService.processAndStoreAsync(file, fileHash, userId);
                    responses.add(response);
                    if (response.getStatus() != null && response.getStatus().canInstantUpload()) {
                        skipCount++;
                    } else {
                        successCount++;
                    }
                } catch (Exception e) {
                    responses.add(buildFailedUploadResponse(file, fileHash, e.getMessage()));
                    failCount++;
                }
            }

            String message = String.format("批量上传完成 (新上传: %d, 已存在: %d, 失败: %d)",
                    successCount, skipCount, failCount);
            return ApiResponse.success(message, responses);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ApiResponse<List<UploadResponse>> validateBatchUpload(MultipartFile[] files, String[] fileHashes) {
        if (files == null || files.length == 0) {
            return ApiResponse.validationError("文件列表不能为空");
        }
        if (fileHashes == null || fileHashes.length == 0) {
            return ApiResponse.validationError("文件哈希列表不能为空");
        }
        if (files.length != fileHashes.length) {
            return ApiResponse.validationError("文件数量与哈希数量不匹配");
        }
        if (files.length > MAX_BATCH_UPLOAD_FILES) {
            return ApiResponse.validationError("单次最多上传 20 个文件");
        }
        return null;
    }

    private UploadResponse buildFailedUploadResponse(MultipartFile file, String fileHash, String errorMessage) {
        return UploadResponse.builder()
                .success(false)
                .fileHash(fileHash)
                .filename(file != null ? file.getOriginalFilename() : null)
                .message("上传失败: " + errorMessage)
                .errorMessage(errorMessage)
                .build();
    }
}
