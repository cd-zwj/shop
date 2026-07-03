package com.payment.dto;

import lombok.Data;

/**
 * 销售分析请求数据传输对象，用于指定商户和时间范围进行销售数据分析。
 */
@Data
public class SalesAnalysisRequest {
    /** 商户 ID */
    private Long merchantId;

    /** 开始日期（格式：yyyy-MM-dd） */
    private String startDate;

    /** 结束日期（格式：yyyy-MM-dd） */
    private String endDate;

    /** 分析类型：trend（趋势分析）、category（品类分析）、customer（客户分析） */
    private String analysisType;
}
