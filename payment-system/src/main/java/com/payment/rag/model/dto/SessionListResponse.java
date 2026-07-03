package com.payment.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 会话列表响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionListResponse {
    private String userId;
    private Set<String> sessions;
    private Integer sessionCount;
    private Long timestamp;
}
