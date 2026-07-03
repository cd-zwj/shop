package com.payment.rag.model.dto;

import com.payment.rag.model.DocumentFileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileExistenceResponse {

    private Boolean exists;
    private String sourceId;
    private String filename;
    private Integer chunkCount;
    private String minioPath;
    private String minioUrl;
    private List<String> unitIds;
    private DocumentFileStatus status;
    private String errorMessage;

    public static FileExistenceResponse notExists() {
        return FileExistenceResponse.builder()
                .exists(false)
                .build();
    }

    public static FileExistenceResponse exists(String sourceId, String filename,
                                               Integer chunkCount, String minioPath,
                                               String minioUrl, List<String> unitIds) {
        return FileExistenceResponse.builder()
                .exists(true)
                .sourceId(sourceId)
                .filename(filename)
                .chunkCount(chunkCount)
                .minioPath(minioPath)
                .minioUrl(minioUrl)
                .unitIds(unitIds)
                .status(DocumentFileStatus.SUCCESS)
                .build();
    }

    public static FileExistenceResponse processing(String sourceId, String filename,
                                                   Integer chunkCount, String minioPath,
                                                   String minioUrl, DocumentFileStatus status) {
        return FileExistenceResponse.builder()
                .exists(false)
                .sourceId(sourceId)
                .filename(filename)
                .chunkCount(chunkCount)
                .minioPath(minioPath)
                .minioUrl(minioUrl)
                .status(status)
                .build();
    }

    public static FileExistenceResponse failed(String sourceId, String filename,
                                               Integer chunkCount, String minioPath,
                                               String minioUrl, String errorMessage) {
        return FileExistenceResponse.builder()
                .exists(false)
                .sourceId(sourceId)
                .filename(filename)
                .chunkCount(chunkCount)
                .minioPath(minioPath)
                .minioUrl(minioUrl)
                .status(DocumentFileStatus.FAILED)
                .errorMessage(errorMessage)
                .build();
    }
}
