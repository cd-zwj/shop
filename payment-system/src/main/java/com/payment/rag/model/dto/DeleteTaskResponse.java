package com.payment.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteTaskResponse {
    private String taskId;
    private String filename;
    private String fileHash;
    private String message;
}
