package com.payment.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeChunkResult {
    private String fileHash;
    private String filename;
    private String sourceId;
    private String status;
    private Boolean success;
    private String message;
    private String errorMessage;
}
