package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.DataAnalysisResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据分析结果数据访问接口，提供数据分析结果表（data_analysis_result）的 CRUD 操作。
 * 存储平台/商户维度的统计分析结果，用于数据看板展示。
 */
@Mapper
public interface DataAnalysisResultMapper extends BaseMapper<DataAnalysisResult> {
}

