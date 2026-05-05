package com.payment.dto;

import lombok.Data;
import java.util.Map;

/**
 * AI对话请求DTO
 */
@Data
public class ChatRequest {
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户消息
     */
    private String message;
    
    /**
     * 上下文信息
     */
    private Map<String, Object> context;
}
