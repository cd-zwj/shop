package com.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.dto.AnalysisRequestDTO;
import com.payment.entity.DataAnalysisResult;

/**
 * 数据分析服务接口
 */
public interface DataAnalysisService extends IService<DataAnalysisResult> {
    
    /**
     * 发起数据分析请求
     */
    DataAnalysisResult analyze(AnalysisRequestDTO request);

    /**
     * 执行已入队的数据分析任务。
     */
    void executeAnalysis(Long resultId, AnalysisRequestDTO request);
    
    /**
     * 查询分析结果
     */
    DataAnalysisResult getAnalysisResult(Long id);
    
    /**
     * 获取分析结果列表（分页）
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<DataAnalysisResult> getAnalysisList(String analysisType, Integer current, Integer size);
}

