package com.payment.rag.model.dto;

import com.payment.rag.model.DocumentFileStatus;
import com.payment.rag.model.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    private boolean success;
    private String message;
    private String sourceId;
    private String fileHash;
    private String filename;
    private SourceType sourceType;
    private DocumentFileStatus status;
    private String errorMessage;
    private int chunksCreated;
    private String minioPath;
    private String minioUrl;
    private List<String> unitIds;

    public static UploadResponse success(String sourceId, String filename, SourceType sourceType,
                                         int chunksCreated, String minioPath, String minioUrl, List<String> unitIds) {
        return UploadResponse.builder()
                .success(true)
                .message("File processed successfully")
                .sourceId(sourceId)
                .filename(filename)
                .sourceType(sourceType)
                .status(DocumentFileStatus.SUCCESS)
                .chunksCreated(chunksCreated)
                .minioPath(minioPath)
                .minioUrl(minioUrl)
                .unitIds(unitIds)
                .build();
    }

    public static UploadResponse error(String message) {
        return UploadResponse.builder()
                .success(false)
                .message(message)
                .status(DocumentFileStatus.FAILED)
                .errorMessage(message)
                .build();
    }
}
