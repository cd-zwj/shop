package com.payment.rag.service;

import com.payment.rag.mapper.AiFeedbackMapper;
import com.payment.rag.model.AiFeedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    private final AiFeedbackMapper aiFeedbackMapper;

    public void submitFeedback(String sessionId, int messageIndex, String userId, String feedbackType) {
        AiFeedback feedback = new AiFeedback();
        feedback.setSessionId(sessionId);
        feedback.setMessageIndex(messageIndex);
        feedback.setUserId(userId);
        feedback.setFeedbackType(feedbackType);
        aiFeedbackMapper.insert(feedback);
        log.info("AI 反馈已记录: sessionId={}, messageIndex={}, userId={}, type={}",
                sessionId, messageIndex, userId, feedbackType);
    }
}
