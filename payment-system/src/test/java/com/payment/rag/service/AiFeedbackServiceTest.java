package com.payment.rag.service;

import com.payment.rag.mapper.AiFeedbackMapper;
import com.payment.rag.model.AiFeedback;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiFeedbackService 测试")
class AiFeedbackServiceTest {

    @Mock
    private AiFeedbackMapper aiFeedbackMapper;

    @InjectMocks
    private AiFeedbackService aiFeedbackService;

    @Test
    @DisplayName("提交反馈正确写入数据库")
    void submitFeedbackPersistsToDatabase() {
        aiFeedbackService.submitFeedback("sess-1", 3, "user-123", "UP");

        ArgumentCaptor<AiFeedback> captor = ArgumentCaptor.forClass(AiFeedback.class);
        verify(aiFeedbackMapper).insert(captor.capture());

        AiFeedback saved = captor.getValue();
        assertNotNull(saved);
        assertEquals("sess-1", saved.getSessionId());
        assertEquals(3, saved.getMessageIndex());
        assertEquals("user-123", saved.getUserId());
        assertEquals("UP", saved.getFeedbackType());
    }

    @Test
    @DisplayName("DOWN 类型反馈正确持久化")
    void downFeedbackPersistsCorrectly() {
        aiFeedbackService.submitFeedback("sess-2", 0, "user-456", "DOWN");

        ArgumentCaptor<AiFeedback> captor = ArgumentCaptor.forClass(AiFeedback.class);
        verify(aiFeedbackMapper).insert(captor.capture());

        assertEquals("DOWN", captor.getValue().getFeedbackType());
    }
}
