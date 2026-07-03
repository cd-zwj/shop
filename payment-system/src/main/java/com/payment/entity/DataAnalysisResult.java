package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据分析结果实体，对应数据库表 data_analysis_result。
 * <p>存储平台AI模块生成的数据分析结果，包括用户行为分析、支付趋势分析、用户分群等。
 * 分析任务异步执行，通过 status 字段跟踪处理进度，
 * 分析完成后 chart_url 指向生成的可视化图表。</p>
 */
@Data
@TableName("data_analysis_result")
public class DataAnalysisResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 分析结果主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID，用于多租户数据隔离
     */
    private Long tenantId;

    /**
     * 分析类型：USER_BEHAVIOR-用户行为分析，PAYMENT_TREND-支付趋势分析，USER_SEGMENT-用户分群分析
     */
    private String analysisType;

    /** 分析结果数据（JSON格式），包含具体分析指标和统计值 */
    private String analysisData;

    /**
     * 图表URL，由AI模块生成的可视化图表存储地址
     */
    private String chartUrl;

    /**
     * 处理状态：PROCESSING-处理中，SUCCESS-成功，FAIL-失败
     */
    private String status;

    /** 创建时间（任务提交时间） */
    private LocalDateTime createTime;

    /** 最后更新时间（任务完成或失败时间） */
    private LocalDateTime updateTime;
}

