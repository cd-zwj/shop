package com.payment.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话创建响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionCreateResponse {
    private String sessionId;
    private Long timestamp;
    private String message;
}
