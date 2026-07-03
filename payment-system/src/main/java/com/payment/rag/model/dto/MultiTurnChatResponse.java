package com.payment.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 多轮对话响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultiTurnChatResponse {
    private String sessionId;
    private Integer turnCount;
    private String reply;
    private Boolean knowledgeUsed;
    private Integer retrievedDocuments;
    private Long timestamp;
}
