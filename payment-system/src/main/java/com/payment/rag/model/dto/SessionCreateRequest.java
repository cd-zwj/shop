package com.payment.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话创建请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionCreateRequest {
    private String userId;
}
