package com.payment.rag.Controller;

import com.payment.rag.model.dto.ApiResponse;
import com.payment.rag.service.AiFeedbackService;
import com.payment.rag.service.AuthContextService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/rag/ai/feedback")
@RequiredArgsConstructor
@Tag(name = "AI 反馈", description = "AI 助手消息反馈（点赞/踩/再生）")
public class AiFeedbackController {

    private final AiFeedbackService aiFeedbackService;
    private final AuthContextService authContextService;

    @PostMapping
    public ApiResponse<String> submitFeedback(@RequestBody FeedbackRequest request) {
        String userId = authContextService.getCurrentUserId();
        aiFeedbackService.submitFeedback(
                request.getSessionId(),
                request.getMessageIndex(),
                userId,
                request.getFeedbackType()
        );
        return ApiResponse.success("反馈已记录", "ok");
    }

    public static class FeedbackRequest {
        private String sessionId;
        private int messageIndex;
        private String feedbackType;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public int getMessageIndex() { return messageIndex; }
        public void setMessageIndex(int messageIndex) { this.messageIndex = messageIndex; }
        public String getFeedbackType() { return feedbackType; }
        public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }
    }
}
