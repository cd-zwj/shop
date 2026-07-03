package com.payment.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.payment.config.RabbitMQConfig;
import com.payment.dto.AnalysisRequestDTO;
import com.payment.service.DataAnalysisService;
import com.payment.service.MessageIdempotentService;
import com.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AI 分析任务消费者
 * <p>
 * 从 {@link RabbitMQConfig#AI_ANALYSIS_QUEUE} 队列中消费 AI 分析请求消息，
 * 调用 {@link DataAnalysisService} 执行分析任务。
 * 通过 {@link MessageIdempotentService} 保障消息幂等性，避免重复分析。
 * </p>
 *
 * @author payment-system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAnalysisConsumer {

    /**
     * 消息幂等服务，用于防止重复消费
     */
    private final MessageIdempotentService messageIdempotentService;

    /**
     * 数据分析服务，执行具体的 AI 分析逻辑
     */
    private final DataAnalysisService dataAnalysisService;

    /**
     * 处理 AI 分析消息
     * <p>
     * 从消息体中解析分析请求参数，校验必填字段后调用分析服务执行。
     * 消费前通过幂等校验避免重复处理，消费后记录幂等状态。
     * </p>
     *
     * @param body 消息体 JSON 字符串，包含 resultId、analysisType、userId、params 等字段
     * @throws IllegalArgumentException 当消息体缺少必填字段时抛出
     */
    @RabbitListener(queues = RabbitMQConfig.AI_ANALYSIS_QUEUE)
    public void handleAiAnalysis(String body) {
        Map<String, Object> payload = JsonUtils.fromJson(body, new TypeReference<Map<String, Object>>() {
        });
        String resultIdText = requireNonBlank(payload, "resultId", body);
        Long resultId = Long.valueOf(resultIdText);
        String messageId = RabbitMQConfig.AI_ANALYSIS_QUEUE + ":" + resultId;

        if (messageIdempotentService.isProcessed(messageId, RabbitMQConfig.AI_ANALYSIS_QUEUE)) {
            log.info("AI 分析消息已处理,跳过 messageId={}", messageId);
            return;
        }

        try {
            dataAnalysisService.executeAnalysis(resultId, buildRequest(payload, body));
            messageIdempotentService.recordSuccess(
                    messageId,
                    RabbitMQConfig.AI_ANALYSIS_QUEUE,
                    body,
                    AiAnalysisConsumer.class.getSimpleName());
        } catch (Exception e) {
            messageIdempotentService.recordFailure(
                    messageId,
                    RabbitMQConfig.AI_ANALYSIS_QUEUE,
                    body,
                    AiAnalysisConsumer.class.getSimpleName(),
                    e.getMessage());
            throw e;
        }
    }

    /**
     * 从消息载荷中构建分析请求 DTO
     *
     * @param payload 解析后的消息体 Map
     * @param body    原始消息体 JSON 字符串（用于异常信息输出）
     * @return 构建完成的分析请求对象
     * @throws IllegalArgumentException 当 analysisType 字段缺失或为空时抛出
     */
    private AnalysisRequestDTO buildRequest(Map<String, Object> payload, String body) {
        AnalysisRequestDTO request = new AnalysisRequestDTO();
        request.setAnalysisType(requireNonBlank(payload, "analysisType", body));
        Object userId = payload.get("userId");
        if (userId != null && !userId.toString().isBlank()) {
            request.setUserId(Long.valueOf(userId.toString()));
        }
        Object params = payload.get("params");
        if (params instanceof Map<?, ?> rawParams) {
            request.setParams(rawParams.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            entry -> String.valueOf(entry.getKey()),
                            Map.Entry::getValue)));
        } else {
            request.setParams(Map.of());
        }
        return request;
    }

    /**
     * 校验消息载荷中的字段非空
     *
     * @param payload   消息载荷 Map
     * @param fieldName 字段名
     * @param body      原始消息体（用于异常信息输出）
     * @return 字段值的字符串表示
     * @throws IllegalArgumentException 当字段不存在或值为空白字符串时抛出
     */
    private String requireNonBlank(Map<String, Object> payload, String fieldName, String body) {
        Object rawValue = payload == null ? null : payload.get(fieldName);
        if (rawValue == null) {
            throw new IllegalArgumentException("AI 分析消息缺少 " + fieldName + ", body=" + body);
        }
        String value = rawValue.toString();
        if (value.isBlank()) {
            throw new IllegalArgumentException("AI 分析消息 " + fieldName + " 为空, body=" + body);
        }
        return value;
    }
}
