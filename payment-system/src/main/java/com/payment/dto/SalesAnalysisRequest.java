package com.payment.dto;

import lombok.Data;

/**
 * 销售分析请求DTO
 */
@Data
public class SalesAnalysisRequest {
    /**
     * 商家ID
     */
    private Long merchantId;
    
    /**
     * 开始日期 (yyyy-MM-dd)
     */
    private String startDate;
    
    /**
     * 结束日期 (yyyy-MM-dd)
     */
    private String endDate;
    
    /**
     * 分析类型: trend(趋势), category(品类), customer(客户)
     */
    private String analysisType;
}
