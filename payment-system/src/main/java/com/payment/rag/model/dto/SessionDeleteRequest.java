package com.payment.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话删除请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionDeleteRequest {
    private String userId;
    private String sessionId;
}
