package com.payment.dto;

import lombok.Data;
import java.util.Map;

/**
 * AI分析请求DTO
 */
@Data
public class AnalysisRequestDTO {
    private String analysisType; // USER_BEHAVIOR, PAYMENT_TREND, USER_SEGMENT
    private Map<String, Object> params; // 分析参数
    private Long userId; // 可选，用于用户相关分析
}

