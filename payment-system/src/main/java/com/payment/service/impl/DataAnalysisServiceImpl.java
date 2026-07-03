package com.payment.service.impl;

import com.payment.util.JsonUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payment.common.BusinessException;
import com.payment.config.RabbitMQConfig;
import com.payment.dto.AnalysisRequestDTO;
import com.payment.entity.DataAnalysisResult;
import com.payment.entity.UserBehaviorLog;
import com.payment.mapper.DataAnalysisResultMapper;
import com.payment.mapper.UserBehaviorLogMapper;
import com.payment.service.DataAnalysisService;
import com.payment.service.OutboxPublisher;
import com.payment.service.outbox.OutboxMessageCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据分析服务实现类。
 * <p>
 * 负责平台数据分析任务的创建与异步执行。创建分析记录后通过 Outbox 模式发送消息到 RabbitMQ，
 * 由消费者异步调用外部 AI 模块完成分析。支持用户行为分析（USER_BEHAVIOR）、支付趋势分析（PAYMENT_TREND）、
 * 用户分群分析（USER_SEGMENT）等多种分析类型。分析结果持久化到 data_analysis_result 表。
 * </p>
 *
 * @see DataAnalysisService
 */
@Slf4j
@Service
public class DataAnalysisServiceImpl extends ServiceImpl<DataAnalysisResultMapper, DataAnalysisResult> implements DataAnalysisService {
    
    @Autowired
    private UserBehaviorLogMapper userBehaviorLogMapper;
    
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OutboxPublisher outboxPublisher;
    
    @Value("${ai.base-url}")
    private String aiBaseUrl;
    
    @Value("${ai.analyze-endpoint}")
    private String analyzeEndpoint;
    
    /**
     * 创建数据分析任务并发布 Outbox 消息。
     * <p>
     * 立即创建状态为 PROCESSING 的分析记录，然后通过 Outbox 模式发送异步消息，
     * 由 MQ 消费者调用 AI 模块执行实际分析。
     *
     * @param request 分析请求参数，包含分析类型、用户 ID 及自定义参数
     * @return 已创建的分析结果记录（状态为 PROCESSING）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataAnalysisResult analyze(AnalysisRequestDTO request) {
        // 创建分析记录
        DataAnalysisResult result = new DataAnalysisResult();
        result.setAnalysisType(request.getAnalysisType());
        result.setStatus("PROCESSING");
        result.setAnalysisData(JsonUtils.toJson(request.getParams()));
        save(result);
        
        publishAiAnalysisOutbox(result.getId(), request);
        
        return result;
    }
    
    /**
     * 异步调用 AI 模块执行分析。
     * <p>
     * 准备分析数据后调用外部 AI 接口，成功则更新分析结果为 SUCCESS，
     * 失败则标记为 FAIL 并重新抛出异常交由消费者处理。
     *
     * @param resultId 分析结果记录 ID
     * @param request  分析请求参数
     */
    @Override
    public void executeAnalysis(Long resultId, AnalysisRequestDTO request) {
        try {
            // 准备分析数据
            Map<String, Object> analysisData = prepareAnalysisData(request);
            
            // 调用AI模块
            Map<String, Object> aiRequest = new HashMap<>();
            aiRequest.put("analysisType", request.getAnalysisType());
            aiRequest.put("data", analysisData);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(aiRequest, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiBaseUrl + analyzeEndpoint,
                    entity,
                    Map.class
            );
            
            // 更新分析结果
            DataAnalysisResult result = getById(resultId);
            if (result != null && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                result.setAnalysisData(JsonUtils.toJson(responseBody.get("analysisData")));
                result.setChartUrl((String) responseBody.get("chartUrl"));
                result.setStatus("SUCCESS");
                result.setUpdateTime(LocalDateTime.now());
                updateById(result);
            }
        } catch (Exception e) {
            log.error("调用AI模块失败", e);
            DataAnalysisResult result = getById(resultId);
            if (result != null) {
                result.setStatus("FAIL");
                result.setUpdateTime(LocalDateTime.now());
                updateById(result);
            }
            throw e;
        }
    }

    /**
     * 异步调用 AI 分析的入口方法，委托给 {@link #executeAnalysis(Long, AnalysisRequestDTO)}。
     *
     * @param resultId 分析结果记录 ID
     * @param request  分析请求参数
     */
    public void callAiModuleAsync(Long resultId, AnalysisRequestDTO request) {
        executeAnalysis(resultId, request);
    }
    
    /**
     * 准备分析数据
     */
    private Map<String, Object> prepareAnalysisData(AnalysisRequestDTO request) {
        Map<String, Object> data = new HashMap<>();
        
        if ("USER_BEHAVIOR".equals(request.getAnalysisType())) {
            // 查询用户行为日志
            LambdaQueryWrapper<UserBehaviorLog> wrapper = new LambdaQueryWrapper<>();
            if (request.getUserId() != null) {
                wrapper.eq(UserBehaviorLog::getUserId, request.getUserId());
            }
            wrapper.orderByDesc(UserBehaviorLog::getCreateTime);
            wrapper.last("LIMIT 1000");
            
            List<UserBehaviorLog> logs = userBehaviorLogMapper.selectList(wrapper);
            data.put("logs", logs);
        } else if ("PAYMENT_TREND".equals(request.getAnalysisType())) {
            // 查询支付趋势数据
            // 这里可以从订单表查询数据
            data.put("timeRange", request.getParams().get("timeRange"));
        } else if ("USER_SEGMENT".equals(request.getAnalysisType())) {
            // 用户分群数据
            data.put("segmentType", request.getParams().get("segmentType"));
        }
        
        return data;
    }
    
    /**
     * 查询分析结果详情。
     *
     * @param id 分析结果记录 ID
     * @return 分析结果实体，不存在时返回 null
     */
    @Override
    public DataAnalysisResult getAnalysisResult(Long id) {
        return getById(id);
    }
    
    /**
     * 分页查询分析结果列表。
     *
     * @param analysisType 分析类型过滤条件，为 null 时不过滤
     * @param current      当前页码
     * @param size         每页条数
     * @return 分析结果分页数据，按创建时间倒序排列
     */
    @Override
    public Page<DataAnalysisResult> getAnalysisList(String analysisType, Integer current, Integer size) {
        Page<DataAnalysisResult> page = new Page<>(current, size);
        LambdaQueryWrapper<DataAnalysisResult> wrapper = new LambdaQueryWrapper<>();
        if (analysisType != null) {
            wrapper.eq(DataAnalysisResult::getAnalysisType, analysisType);
        }
        wrapper.orderByDesc(DataAnalysisResult::getCreateTime);
        return page(page, wrapper);
    }

    /**
     * 通过 Outbox 模式发布 AI 分析任务消息到 RabbitMQ。
     *
     * @param resultId 分析结果记录 ID
     * @param request  分析请求参数
     */
    private void publishAiAnalysisOutbox(Long resultId, AnalysisRequestDTO request) {
        outboxPublisher.publish(OutboxMessageCommand.builder()
                .messagePrefix("AI")
                .bizType("AI_ANALYSIS")
                .bizNo(String.valueOf(resultId))
                .routingKey(RabbitMQConfig.AI_ANALYSIS_QUEUE)
                .messageBody(Map.of(
                        "resultId", resultId,
                        "analysisType", request.getAnalysisType(),
                        "userId", request.getUserId() == null ? "" : request.getUserId(),
                        "params", request.getParams() == null ? Map.of() : request.getParams()))
                .build());
    }
}


