package com.payment.service.impl;

import com.payment.config.RabbitMQConfig;
import com.payment.dto.AnalysisRequestDTO;
import com.payment.entity.DataAnalysisResult;
import com.payment.mapper.DataAnalysisResultMapper;
import com.payment.mapper.UserBehaviorLogMapper;
import com.payment.service.OutboxPublisher;
import com.payment.service.outbox.OutboxMessageCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataAnalysisServiceImplTest {

    @Test
    void analyzeShouldCreateProcessingResultAndPublishAiAnalysisOutbox() {
        DataAnalysisResultMapper resultMapper = mock(DataAnalysisResultMapper.class);
        UserBehaviorLogMapper userBehaviorLogMapper = mock(UserBehaviorLogMapper.class);
        OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
        DataAnalysisServiceImpl service = service(resultMapper, userBehaviorLogMapper, outboxPublisher);
        doAnswer(invocation -> {
            DataAnalysisResult result = invocation.getArgument(0);
            result.setId(88L);
            return 1;
        }).when(resultMapper).insert(any(DataAnalysisResult.class));
        AnalysisRequestDTO request = new AnalysisRequestDTO();
        request.setAnalysisType("PAYMENT_TREND");
        request.setUserId(100L);
        request.setParams(Map.of("timeRange", "LAST_7_DAYS"));

        DataAnalysisResult result = service.analyze(request);

        assertEquals(88L, result.getId());
        assertEquals("PROCESSING", result.getStatus());
        ArgumentCaptor<OutboxMessageCommand> commandCaptor = ArgumentCaptor.forClass(OutboxMessageCommand.class);
        verify(outboxPublisher).publish(commandCaptor.capture());
        OutboxMessageCommand command = commandCaptor.getValue();
        assertEquals("AI_ANALYSIS", command.getBizType());
        assertEquals("88", command.getBizNo());
        assertEquals(RabbitMQConfig.AI_ANALYSIS_QUEUE, command.getRoutingKey());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) command.getMessageBody();
        assertEquals(88L, body.get("resultId"));
        assertEquals("PAYMENT_TREND", body.get("analysisType"));
        assertEquals(100L, body.get("userId"));
        assertEquals(Map.of("timeRange", "LAST_7_DAYS"), body.get("params"));
    }

    @Test
    void executeAnalysisShouldMarkFailedAndRethrowWhenAiCallFails() {
        DataAnalysisResultMapper resultMapper = mock(DataAnalysisResultMapper.class);
        UserBehaviorLogMapper userBehaviorLogMapper = mock(UserBehaviorLogMapper.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        DataAnalysisServiceImpl service = service(resultMapper, userBehaviorLogMapper, mock(OutboxPublisher.class));
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(service, "aiBaseUrl", "http://ai");
        ReflectionTestUtils.setField(service, "analyzeEndpoint", "/analyze");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class)))
                .thenThrow(new IllegalStateException("ai down"));
        DataAnalysisResult existing = new DataAnalysisResult();
        existing.setId(88L);
        existing.setStatus("PROCESSING");
        when(resultMapper.selectById(88L)).thenReturn(existing);
        AnalysisRequestDTO request = new AnalysisRequestDTO();
        request.setAnalysisType("PAYMENT_TREND");
        request.setParams(Map.of("timeRange", "LAST_7_DAYS"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.executeAnalysis(88L, request));

        assertEquals("ai down", ex.getMessage());
        ArgumentCaptor<DataAnalysisResult> resultCaptor = ArgumentCaptor.forClass(DataAnalysisResult.class);
        verify(resultMapper).updateById(resultCaptor.capture());
        assertEquals(88L, resultCaptor.getValue().getId());
        assertEquals("FAIL", resultCaptor.getValue().getStatus());
    }

    private DataAnalysisServiceImpl service(DataAnalysisResultMapper resultMapper,
                                            UserBehaviorLogMapper userBehaviorLogMapper,
                                            OutboxPublisher outboxPublisher) {
        DataAnalysisServiceImpl service = new DataAnalysisServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", resultMapper);
        ReflectionTestUtils.setField(service, "userBehaviorLogMapper", userBehaviorLogMapper);
        ReflectionTestUtils.setField(service, "outboxPublisher", outboxPublisher);
        return service;
    }
}
