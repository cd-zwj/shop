package com.payment.rag.service;

import com.payment.rag.model.dto.ApiResponse;
import com.payment.rag.model.dto.DeleteTaskResponse;
import com.payment.rag.model.dto.DeleteTaskStatus;
import com.payment.rag.model.dto.DocumentFileStatusResponse;
import com.payment.rag.model.dto.PageRequest;
import com.payment.rag.model.dto.PageResponse;
import com.payment.rag.model.dto.RagDocumentInfo;
import com.payment.rag.util.HashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class RagDocumentApplicationService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt");
    private static final Set<String> ALLOWED_SORT_ORDERS = Set.of("ASC", "DESC");

    private final RagUnitService ragUnitService;
    private final DocumentDeleteService documentDeleteService;

    public ApiResponse<PageResponse<RagDocumentInfo>> getDocuments(Integer page,
                                                                   Integer pageSize,
                                                                   String userId,
                                                                   String sourceType,
                                                                   String keyword,
                                                                   String sortBy,
                                                                   String sortOrder) {
        if (page < 1) {
            return ApiResponse.validationError("页码必须大于 0");
        }
        if (pageSize < 1 || pageSize > 100) {
            return ApiResponse.validationError("每页大小必须在 1-100 之间");
        }

        try {
            String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
            String safeSortOrder = (sortOrder != null && ALLOWED_SORT_ORDERS.contains(sortOrder.toUpperCase()))
                    ? sortOrder.toUpperCase() : "DESC";

            PageRequest request = new PageRequest();
            request.setPage(page);
            request.setPageSize(pageSize);
            request.setUserId(userId);
            request.setSourceType(sourceType);
            request.setKeyword(keyword);
            request.setSortBy(safeSortBy);
            request.setSortOrder(safeSortOrder);
            return ApiResponse.success("查询成功", ragUnitService.getDocumentsPage(request));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ApiResponse<DocumentFileStatusResponse> getDocumentStatus(String fileHash, String userId) {
        if (!isValidSha256(fileHash)) {
            return ApiResponse.validationError("无效的 SHA-256 哈希值");
        }

        try {
            return ApiResponse.success("查询成功", ragUnitService.getDocumentStatus(userId, fileHash));
        } catch (RuntimeException e) {
            if (containsNotFoundMessage(e)) {
                return ApiResponse.notFound("文档");
            }
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ApiResponse<DeleteTaskResponse> deleteDocument(String fileHash, String userId) {
        if (!isValidSha256(fileHash)) {
            return ApiResponse.validationError("无效的 SHA-256 哈希值");
        }

        try {
            String taskId = ragUnitService.deleteDocumentAsync(userId, fileHash);
            return ApiResponse.success("删除任务已提交", DeleteTaskResponse.builder()
                    .taskId(taskId)
                    .fileHash(fileHash)
                    .message("删除任务已提交，正在后台处理中...")
                    .build());
        } catch (RuntimeException e) {
            if (containsNotFoundMessage(e)) {
                return ApiResponse.notFound("文档");
            }
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ApiResponse<DeleteTaskResponse> deleteDocumentByFilename(String userId, String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return ApiResponse.validationError("文件名不能为空");
        }

        try {
            String taskId = ragUnitService.deleteDocumentAsyncByFilename(userId, filename);
            return ApiResponse.success("删除任务已提交", DeleteTaskResponse.builder()
                    .taskId(taskId)
                    .filename(filename)
                    .message("删除任务已提交，正在后台处理中...")
                    .build());
        } catch (RuntimeException e) {
            if (containsNotFoundMessage(e)) {
                return ApiResponse.notFound("文档");
            }
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ApiResponse<DeleteTaskStatus> getDeleteStatus(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return ApiResponse.validationError("任务 ID 不能为空");
        }

        try {
            return ApiResponse.success("查询成功", documentDeleteService.getDeleteStatus(taskId));
        } catch (RuntimeException e) {
            if (containsExpiredOrNotFoundMessage(e)) {
                return ApiResponse.notFound("删除任务");
            }
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isValidSha256(String fileHash) {
        return HashUtils.isValidSha256(fileHash);
    }

    private boolean containsNotFoundMessage(RuntimeException exception) {
        return exception.getMessage() != null && exception.getMessage().contains("不存在");
    }

    private boolean containsExpiredOrNotFoundMessage(RuntimeException exception) {
        return exception.getMessage() != null
                && (exception.getMessage().contains("不存在") || exception.getMessage().contains("过期"));
    }
}
