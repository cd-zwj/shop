package com.payment.consumer;

import com.payment.config.RabbitMQConfig;
import com.payment.dto.AnalysisRequestDTO;
import com.payment.service.DataAnalysisService;
import com.payment.service.MessageIdempotentService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static com.payment.service.MessageClaim.acquired;
import static com.payment.service.MessageClaim.completed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAnalysisConsumerTest {

    private final MessageIdempotentService messageIdempotentService = mock(MessageIdempotentService.class);
    private final DataAnalysisService dataAnalysisService = mock(DataAnalysisService.class);
    private final AiAnalysisConsumer consumer = new AiAnalysisConsumer(messageIdempotentService, dataAnalysisService);

    @Test
    void handleAiAnalysisShouldRejectMissingResultId() {
        String body = "{\"analysisType\":\"PAYMENT_TREND\",\"params\":{\"timeRange\":\"LAST_7_DAYS\"}}";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> consumer.handleAiAnalysis(body));

        assert ex.getMessage().contains("resultId");
        verify(dataAnalysisService, never()).executeAnalysis(any(), any());
        verify(messageIdempotentService, never()).recordSuccess(any(), any(), any(), any(), any());
    }

    @Test
    void handleAiAnalysisShouldSkipProcessedMessage() {
        String body = "{\"resultId\":88,\"analysisType\":\"PAYMENT_TREND\",\"userId\":100,"
                + "\"params\":{\"timeRange\":\"LAST_7_DAYS\"}}";
        String messageId = RabbitMQConfig.AI_ANALYSIS_QUEUE + ":88";
        when(messageIdempotentService.tryClaim(
                messageId, RabbitMQConfig.AI_ANALYSIS_QUEUE, body,
                AiAnalysisConsumer.class.getSimpleName())).thenReturn(completed());

        consumer.handleAiAnalysis(body);

        verify(dataAnalysisService, never()).executeAnalysis(any(), any());
        verify(messageIdempotentService, never()).recordSuccess(any(), any(), any(), any(), any());
    }

    @Test
    void handleAiAnalysisShouldExecuteAnalysisAndRecordSuccess() {
        String body = "{\"resultId\":88,\"analysisType\":\"PAYMENT_TREND\",\"userId\":100,"
                + "\"params\":{\"timeRange\":\"LAST_7_DAYS\"}}";
        String messageId = RabbitMQConfig.AI_ANALYSIS_QUEUE + ":88";
        when(messageIdempotentService.tryClaim(
                messageId, RabbitMQConfig.AI_ANALYSIS_QUEUE, body,
                AiAnalysisConsumer.class.getSimpleName())).thenReturn(acquired("claim-token"));

        consumer.handleAiAnalysis(body);

        ArgumentCaptor<AnalysisRequestDTO> requestCaptor = ArgumentCaptor.forClass(AnalysisRequestDTO.class);
        verify(dataAnalysisService).executeAnalysis(eqLong(88L), requestCaptor.capture());
        assertEquals("PAYMENT_TREND", requestCaptor.getValue().getAnalysisType());
        assertEquals(100L, requestCaptor.getValue().getUserId());
        assertEquals("LAST_7_DAYS", requestCaptor.getValue().getParams().get("timeRange"));
        verify(messageIdempotentService).recordSuccess(
                messageId,
                RabbitMQConfig.AI_ANALYSIS_QUEUE,
                body,
                AiAnalysisConsumer.class.getSimpleName(),
                "claim-token");
    }

    @Test
    void handleAiAnalysisShouldRecordFailureAndRethrow() {
        String body = "{\"resultId\":88,\"analysisType\":\"PAYMENT_TREND\",\"params\":{\"timeRange\":\"LAST_7_DAYS\"}}";
        String messageId = RabbitMQConfig.AI_ANALYSIS_QUEUE + ":88";
        when(messageIdempotentService.tryClaim(
                messageId, RabbitMQConfig.AI_ANALYSIS_QUEUE, body,
                AiAnalysisConsumer.class.getSimpleName())).thenReturn(acquired("claim-token"));
        doThrow(new IllegalStateException("ai failed")).when(dataAnalysisService).executeAnalysis(any(), any());

        assertThrows(IllegalStateException.class, () -> consumer.handleAiAnalysis(body));

        verify(messageIdempotentService).recordFailure(
                messageId,
                RabbitMQConfig.AI_ANALYSIS_QUEUE,
                body,
                AiAnalysisConsumer.class.getSimpleName(),
                "claim-token",
                "ai failed");
    }

    private Long eqLong(Long value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
