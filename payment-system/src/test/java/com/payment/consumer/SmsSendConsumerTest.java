package com.payment.consumer;

import com.payment.config.RabbitMQConfig;
import com.payment.service.MessageIdempotentService;
import com.payment.service.sms.SmsSender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmsSendConsumerTest {

    private final MessageIdempotentService messageIdempotentService = mock(MessageIdempotentService.class);
    private final SmsSender smsSender = mock(SmsSender.class);
    private final SmsSendConsumer consumer = new SmsSendConsumer(messageIdempotentService, smsSender);

    @Test
    void handleSmsSendShouldRejectMissingPhone() {
        String body = "{\"scene\":\"LOGIN_CODE\",\"code\":\"123456\"}";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> consumer.handleSmsSend(body));

        assert ex.getMessage().contains("phone");
        verify(smsSender, never()).send(anyString(), anyString());
        verify(messageIdempotentService, never()).recordSuccess(any(), any(), any(), any());
    }

    @Test
    void handleSmsSendShouldSkipProcessedMessage() {
        String body = "{\"scene\":\"LOGIN_CODE\",\"phone\":\"13800000000\",\"code\":\"123456\"}";
        String messageId = RabbitMQConfig.SMS_SEND_QUEUE + ":LOGIN_CODE:13800000000:123456";
        when(messageIdempotentService.isProcessed(messageId, RabbitMQConfig.SMS_SEND_QUEUE))
                .thenReturn(true);

        consumer.handleSmsSend(body);

        verify(smsSender, never()).send(anyString(), anyString());
        verify(messageIdempotentService, never()).recordSuccess(any(), any(), any(), any());
    }

    @Test
    void handleSmsSendShouldCallSenderAndRecordSuccess() {
        String body = "{\"scene\":\"LOGIN_CODE\",\"phone\":\"13800000000\",\"code\":\"123456\"}";
        String messageId = RabbitMQConfig.SMS_SEND_QUEUE + ":LOGIN_CODE:13800000000:123456";
        when(messageIdempotentService.isProcessed(messageId, RabbitMQConfig.SMS_SEND_QUEUE))
                .thenReturn(false);

        consumer.handleSmsSend(body);

        verify(smsSender).send("13800000000", "123456");
        verify(messageIdempotentService).recordSuccess(
                messageId,
                RabbitMQConfig.SMS_SEND_QUEUE,
                body,
                SmsSendConsumer.class.getSimpleName());
    }

    @Test
    void handleSmsSendShouldRecordFailureAndRethrowWhenSenderFails() {
        String body = "{\"scene\":\"LOGIN_CODE\",\"phone\":\"13800000000\",\"code\":\"123456\"}";
        String messageId = RabbitMQConfig.SMS_SEND_QUEUE + ":LOGIN_CODE:13800000000:123456";
        when(messageIdempotentService.isProcessed(messageId, RabbitMQConfig.SMS_SEND_QUEUE))
                .thenReturn(false);
        doThrow(new IllegalStateException("provider failed")).when(smsSender).send("13800000000", "123456");

        assertThrows(IllegalStateException.class, () -> consumer.handleSmsSend(body));

        verify(messageIdempotentService).recordFailure(
                messageId,
                RabbitMQConfig.SMS_SEND_QUEUE,
                body,
                SmsSendConsumer.class.getSimpleName(),
                "provider failed");
    }
}
