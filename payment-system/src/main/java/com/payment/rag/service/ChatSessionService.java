package com.payment.rag.service;

import com.payment.rag.Config.SessionManager;
import com.payment.rag.Config.SummaryWindowChatMemory;
import com.payment.rag.model.dto.ApiResponse;
import com.payment.rag.model.dto.SessionCreateRequest;
import com.payment.rag.model.dto.SessionCreateResponse;
import com.payment.rag.model.dto.SessionDeleteRequest;
import com.payment.rag.model.dto.SessionDeleteResponse;
import com.payment.rag.model.dto.SessionListRequest;
import com.payment.rag.model.dto.SessionListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 会话管理服务，负责会话的创建/查询/删除和历史记录管理。
 * <p>
 * 从 AiService 中拆分而来，AiService 保留纯聊天编排职责。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final SessionManager sessionManager;
    private final SummaryWindowChatMemory chatMemory;
    private final UserProfileService userProfileService;

    public ApiResponse<SessionCreateResponse> createSession(SessionCreateRequest request) {
        String sessionId = sessionManager.createSession(request.getUserId());
        SessionCreateResponse data = new SessionCreateResponse(
                sessionId,
                System.currentTimeMillis(),
                "会话创建成功"
        );
        return ApiResponse.success(data);
    }

    public ApiResponse<SessionListResponse> getUserSessions(SessionListRequest request) {
        Set<String> sessions = sessionManager.getUserSessions(request.getUserId());
        SessionListResponse data = new SessionListResponse(
                request.getUserId(),
                sessions,
                sessions.size(),
                System.currentTimeMillis()
        );
        return ApiResponse.success(data);
    }

    public ApiResponse<SessionDeleteResponse> deleteSession(SessionDeleteRequest request) {
        validateSessionOwnership(request.getUserId(), request.getSessionId());
        List<Message> history = getHistoryMessages(request.getSessionId());
        submitProfileExtractionIfNeeded(request.getUserId(), history);
        chatMemory.clear(request.getSessionId());
        sessionManager.deleteSession(request.getUserId(), request.getSessionId());

        SessionDeleteResponse data = new SessionDeleteResponse(
                request.getSessionId(),
                System.currentTimeMillis(),
                "会话删除成功，画像提炼已在后台进行"
        );
        return ApiResponse.success(data);
    }

    public ApiResponse<String> extractProfile(SessionDeleteRequest request) {
        if (!isSessionOwnedByUser(request.getUserId(), request.getSessionId())) {
            return ApiResponse.success("会话无效，可能已过期或不存在");
        }
        List<Message> history = getHistoryMessages(request.getSessionId());
        submitProfileExtractionIfNeeded(request.getUserId(), history);
        return ApiResponse.success("画像提炼任务已提交");
    }

    public ApiResponse<List<Map<String, Object>>> getHistory(String userId, String sessionId) {
        validateSessionOwnership(userId, sessionId);
        List<Map<String, Object>> rawHistory = chatMemory.getFullHistoryDetail(sessionId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : rawHistory) {
            String type = (String) item.get("type");
            if (type == null) continue;
            String role = switch (type.toLowerCase()) {
                case "user" -> "user";
                case "assistant" -> "ai";
                default -> null;
            };
            if (role == null) continue;
            result.add(Map.of(
                    "role", role,
                    "content", item.getOrDefault("content", "")
            ));
        }
        return ApiResponse.success(result);
    }

    /**
     * 验证会话归属并返回 userId，同时刷新活跃时间。
     * 供 AiService 多轮对话使用。
     */
    public String requireActiveSessionUser(String sessionId) {
        if (!sessionManager.sessionExists(sessionId)) {
            throw new IllegalArgumentException("会话不存在或已过期");
        }
        String userId = sessionManager.getUserIdBySession(sessionId);
        if (userId == null) {
            throw new IllegalArgumentException("无法获取会话用户信息");
        }
        sessionManager.updateSessionActivity(userId, sessionId);
        return userId;
    }

    public List<Message> getHistoryMessages(String sessionId) {
        return chatMemory.getFullHistory(sessionId);
    }

    private boolean isSessionOwnedByUser(String userId, String sessionId) {
        String sessionUserId = sessionManager.getUserIdBySession(sessionId);
        return sessionUserId != null && sessionUserId.equals(userId);
    }

    private void validateSessionOwnership(String userId, String sessionId) {
        if (!isSessionOwnedByUser(userId, sessionId)) {
            throw new IllegalArgumentException("会话不存在或无权删除");
        }
    }

    private void submitProfileExtractionIfNeeded(String userId, List<Message> history) {
        if (history != null && !history.isEmpty()) {
            userProfileService.extractProfileAsync(userId, history);
        }
    }
}
