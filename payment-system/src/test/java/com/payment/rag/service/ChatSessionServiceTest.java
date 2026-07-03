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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import org.springframework.ai.chat.messages.Message;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatSessionService 测试")
class ChatSessionServiceTest {

    @Mock
    private SessionManager sessionManager;
    @Mock
    private SummaryWindowChatMemory chatMemory;
    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private ChatSessionService chatSessionService;

    @Nested
    @DisplayName("创建会话")
    class CreateSession {

        @Test
        @DisplayName("正常创建会话返回 sessionId")
        void createSessionReturnsSessionId() {
            when(sessionManager.createSession("user-1")).thenReturn("sess-123");

            ApiResponse<SessionCreateResponse> response =
                    chatSessionService.createSession(new SessionCreateRequest("user-1"));

            assertNotNull(response.getData());
            assertEquals("sess-123", response.getData().getSessionId());
            assertEquals("会话创建成功", response.getData().getMessage());
            verify(sessionManager).createSession("user-1");
        }
    }

    @Nested
    @DisplayName("查询会话列表")
    class GetUserSessions {

        @Test
        @DisplayName("返回用户所有会话")
        void returnsAllUserSessions() {
            when(sessionManager.getUserSessions("user-1")).thenReturn(Set.of("s1", "s2"));

            ApiResponse<SessionListResponse> response =
                    chatSessionService.getUserSessions(new SessionListRequest("user-1"));

            assertNotNull(response.getData());
            assertEquals(2, response.getData().getSessionCount());
            assertEquals("user-1", response.getData().getUserId());
        }

        @Test
        @DisplayName("用户无会话时返回空集合")
        void returnsEmptyWhenNoSessions() {
            when(sessionManager.getUserSessions("user-1")).thenReturn(Set.of());

            ApiResponse<SessionListResponse> response =
                    chatSessionService.getUserSessions(new SessionListRequest("user-1"));

            assertEquals(0, response.getData().getSessionCount());
        }
    }

    @Nested
    @DisplayName("删除会话")
    class DeleteSession {

        @Test
        @DisplayName("正常删除会话并清除记忆")
        void deleteSessionClearsMemoryAndRemovesSession() {
            when(sessionManager.getUserIdBySession("s1")).thenReturn("user-1");
            when(chatMemory.getFullHistory("s1")).thenReturn(List.of());

            ApiResponse<SessionDeleteResponse> response =
                    chatSessionService.deleteSession(new SessionDeleteRequest("user-1", "s1"));

            assertEquals("s1", response.getData().getSessionId());
            verify(chatMemory).clear("s1");
            verify(sessionManager).deleteSession("user-1", "s1");
        }

        @Test
        @DisplayName("删除非自己的会话抛出异常")
        void deleteOtherUsersSessionThrows() {
            when(sessionManager.getUserIdBySession("s1")).thenReturn("other-user");

            assertThrows(IllegalArgumentException.class,
                    () -> chatSessionService.deleteSession(new SessionDeleteRequest("user-1", "s1")));
        }

        @Test
        @DisplayName("删除不存在的会话抛出异常")
        void deleteNonexistentSessionThrows() {
            when(sessionManager.getUserIdBySession("s1")).thenReturn(null);

            assertThrows(IllegalArgumentException.class,
                    () -> chatSessionService.deleteSession(new SessionDeleteRequest("user-1", "s1")));
        }

        @Test
        @DisplayName("有历史记录时触发画像提炼")
        void deleteWithHistoryTriggersProfileExtraction() {
            when(sessionManager.getUserIdBySession("s1")).thenReturn("user-1");
            List<Message> history = List.of(
                    new org.springframework.ai.chat.messages.UserMessage("hello")
            );
            when(chatMemory.getFullHistory("s1")).thenReturn(history);

            chatSessionService.deleteSession(new SessionDeleteRequest("user-1", "s1"));

            verify(userProfileService).extractProfileAsync(eq("user-1"), eq(history));
        }
    }

    @Nested
    @DisplayName("requireActiveSessionUser")
    class RequireActiveSessionUser {

        @Test
        @DisplayName("会话不存在时抛出异常")
        void throwsWhenSessionNotExists() {
            when(sessionManager.sessionExists("s1")).thenReturn(false);

            assertThrows(IllegalArgumentException.class,
                    () -> chatSessionService.requireActiveSessionUser("s1"));
        }

        @Test
        @DisplayName("会话存在但无用户信息时抛出异常")
        void throwsWhenNoUserForSession() {
            when(sessionManager.sessionExists("s1")).thenReturn(true);
            when(sessionManager.getUserIdBySession("s1")).thenReturn(null);

            assertThrows(IllegalArgumentException.class,
                    () -> chatSessionService.requireActiveSessionUser("s1"));
        }

        @Test
        @DisplayName("正常返回 userId 并刷新活跃时间")
        void returnsUserIdAndRefreshesActivity() {
            when(sessionManager.sessionExists("s1")).thenReturn(true);
            when(sessionManager.getUserIdBySession("s1")).thenReturn("user-1");

            String userId = chatSessionService.requireActiveSessionUser("s1");

            assertEquals("user-1", userId);
            verify(sessionManager).updateSessionActivity("user-1", "s1");
        }
    }
}
