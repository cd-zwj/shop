package com.payment.service.impl;

import com.payment.util.JsonUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payment.common.BusinessException;
import com.payment.dto.AnalysisRequestDTO;
import com.payment.entity.DataAnalysisResult;
import com.payment.entity.UserBehaviorLog;
import com.payment.mapper.DataAnalysisResultMapper;
import com.payment.mapper.UserBehaviorLogMapper;
import com.payment.service.DataAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据分析服务实现类
 */
@Slf4j
@Service
public class DataAnalysisServiceImpl extends ServiceImpl<DataAnalysisResultMapper, DataAnalysisResult> implements DataAnalysisService {
    
    @Autowired
    private UserBehaviorLogMapper userBehaviorLogMapper;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${ai.base-url}")
    private String aiBaseUrl;
    
    @Value("${ai.analyze-endpoint}")
    private String analyzeEndpoint;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataAnalysisResult analyze(AnalysisRequestDTO request) {
        // 创建分析记录
        DataAnalysisResult result = new DataAnalysisResult();
        result.setAnalysisType(request.getAnalysisType());
        result.setStatus("PROCESSING");
        result.setAnalysisData(JsonUtils.toJson(request.getParams()));
        save(result);
        
        // 异步调用AI模块
        callAiModuleAsync(result.getId(), request);
        
        return result;
    }
    
    /**
     * 异步调用AI模块
     */
    @Async
    public void callAiModuleAsync(Long resultId, AnalysisRequestDTO request) {
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
        }
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
    
    @Override
    public DataAnalysisResult getAnalysisResult(Long id) {
        return getById(id);
    }
    
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
}


