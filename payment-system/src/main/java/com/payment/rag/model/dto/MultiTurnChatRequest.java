package com.payment.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 多轮对话请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultiTurnChatRequest {
    private String userId;
    private String sessionId;
    private Integer turnCount;
    private String message;
    private AiScenario scenario = AiScenario.GENERAL_RAG_QA;
    private AgentMode modeHint = AgentMode.AUTO;
    private String approvedPlanId;
}
