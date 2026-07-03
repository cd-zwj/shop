package com.payment.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话列表请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionListRequest {
    private String userId;
}
