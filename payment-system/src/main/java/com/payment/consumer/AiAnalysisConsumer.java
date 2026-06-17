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
 * AI 分析任务消费者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAnalysisConsumer {

    private final MessageIdempotentService messageIdempotentService;
    private final DataAnalysisService dataAnalysisService;

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
