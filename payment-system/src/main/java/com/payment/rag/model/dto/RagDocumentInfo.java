package com.payment.rag.model.dto;

import com.payment.rag.model.DocumentFileStatus;
import com.payment.rag.model.SourceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagDocumentInfo {
    private String sourceId;
    private String fileHash;
    private String userId;
    private String filename;
    private SourceType sourceType;
    private String minioUrl;
    private String minioPath;
    private Long fileSize;
    private DocumentFileStatus status;
    private String errorMessage;
    private Integer chunkCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
