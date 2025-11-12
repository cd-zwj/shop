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
     * 查询分析结果
     */
    DataAnalysisResult getAnalysisResult(Long id);
    
    /**
     * 获取分析结果列表
     */
    java.util.List<DataAnalysisResult> getAnalysisList(String analysisType);
}

