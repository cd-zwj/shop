package com.payment.dto;

import lombok.Data;
import java.util.Map;

/**
 * AI 分析请求数据传输对象，用于向 AI 分析引擎提交分析任务（用户行为、支付趋势、用户分群等）。
 */
@Data
public class AnalysisRequestDTO {
    /** 分析类型：USER_BEHAVIOR（用户行为）、PAYMENT_TREND（支付趋势）、USER_SEGMENT（用户分群） */
    private String analysisType;
    /** 分析参数（键值对，具体参数取决于分析类型） */
    private Map<String, Object> params;
    /** 用户 ID（可选，用于用户维度的分析） */
    private Long userId;
}

