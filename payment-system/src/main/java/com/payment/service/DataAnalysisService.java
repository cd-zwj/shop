package com.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.dto.AnalysisRequestDTO;
import com.payment.entity.DataAnalysisResult;

/**
 * 数据分析服务接口。
 *
 * <p>提供异步数据分析能力，支持发起分析请求、执行分析任务、查询分析结果等功能。
 * 分析任务通过消息队列异步执行，结果持久化到数据库中供后续查阅。</p>
 */
public interface DataAnalysisService extends IService<DataAnalysisResult> {

    /**
     * 发起数据分析请求（提交到异步队列）。
     *
     * @param request 分析请求DTO，包含分析类型、时间范围、筛选条件等
     * @return 创建的分析结果记录（状态为等待执行）
     * @throws com.payment.common.exception.BusinessException 当请求参数校验失败时抛出
     */
    DataAnalysisResult analyze(AnalysisRequestDTO request);

    /**
     * 执行已入队的数据分析任务（由消息队列消费者调用）。
     *
     * @param resultId 分析结果记录ID
     * @param request  分析请求参数
     */
    void executeAnalysis(Long resultId, AnalysisRequestDTO request);

    /**
     * 查询分析结果详情。
     *
     * @param id 分析结果ID
     * @return 分析结果实体，包含分析状态和结果数据
     * @throws com.payment.common.exception.BusinessException 当结果不存在时抛出
     */
    DataAnalysisResult getAnalysisResult(Long id);

    /**
     * 分页查询分析结果列表。
     *
     * @param analysisType 分析类型过滤（可空）
     * @param current      页码
     * @param size         每页条数
     * @return 分析结果分页数据
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<DataAnalysisResult> getAnalysisList(String analysisType, Integer current, Integer size);
}
