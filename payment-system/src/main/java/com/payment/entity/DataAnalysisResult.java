package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据分析结果实体
 */
@Data
@TableName("data_analysis_result")
public class DataAnalysisResult implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 分析类型：USER_BEHAVIOR-用户行为，PAYMENT_TREND-支付趋势，USER_SEGMENT-用户分群
     */
    private String analysisType;
    
    private String analysisData;
    
    /**
     * 图表URL（AI模块生成）
     */
    private String chartUrl;
    
    /**
     * 状态：PROCESSING-处理中，SUCCESS-成功，FAIL-失败
     */
    private String status;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}

