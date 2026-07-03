package com.payment.rag.service;

import com.payment.rag.Config.DateTimeTools;
import com.payment.rag.model.dto.ApiResponse;
import com.payment.rag.model.dto.MultiTurnChatRequest;
import com.payment.rag.model.dto.RetrievalResult;
import com.payment.rag.model.dto.SessionCreateRequest;
import com.payment.rag.model.dto.SessionCreateResponse;
import com.payment.rag.model.dto.SessionDeleteRequest;
import com.payment.rag.model.dto.SessionDeleteResponse;
import com.payment.rag.model.dto.SessionListRequest;
import com.payment.rag.model.dto.SessionListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.payment.rag.service.agent.AgentOrchestrator;
import com.payment.rag.service.agent.ReactAgentExecutor;
import com.payment.rag.service.scenario.ScenarioPromptService;
import com.payment.rag.service.scenario.ScenarioBusinessTools;
import com.payment.rag.service.scenario.ScenarioToolExposureService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Slf4j
@Service
public class AiService {

    private final ChatClient deepchatClient;
    private final RagRetrievalService ragRetrievalService;
    private final UserProfileService userProfileService;
    private final DateTimeTools dateTimeTools;
    private final ChatSessionService chatSessionService;
    private final AgentOrchestrator agentOrchestrator;
    private final ReactAgentExecutor legacyReactAgentExecutor;

    @Autowired
    public AiService(ChatClient deepchatClient,
                     RagRetrievalService ragRetrievalService,
                     QueryRewriteService queryRewriteService,
                     RetrievalSubQueryService retrievalSubQueryService,
                     UserProfileService userProfileService,
                     DateTimeTools dateTimeTools,
                     ChatSessionService chatSessionService,
                     AgentOrchestrator agentOrchestrator,
                     ScenarioPromptService scenarioPromptService,
                     ScenarioBusinessTools scenarioBusinessTools,
                     ScenarioToolExposureService scenarioToolExposureService) {
        this.deepchatClient = deepchatClient;
        this.ragRetrievalService = ragRetrievalService;
        this.userProfileService = userProfileService;
        this.dateTimeTools = dateTimeTools;
        this.chatSessionService = chatSessionService;
        this.agentOrchestrator = agentOrchestrator;
        this.legacyReactAgentExecutor = null;
    }

    public AiService(ChatClient deepchatClient,
                     RagRetrievalService ragRetrievalService,
                     QueryRewriteService queryRewriteService,
                     RetrievalSubQueryService retrievalSubQueryService,
                     UserProfileService userProfileService,
                     DateTimeTools dateTimeTools,
                     ChatSessionService chatSessionService,
                     ScenarioPromptService scenarioPromptService,
                     ScenarioBusinessTools scenarioBusinessTools,
                     ScenarioToolExposureService scenarioToolExposureService) {
        this.deepchatClient = deepchatClient;
        this.ragRetrievalService = ragRetrievalService;
        this.userProfileService = userProfileService;
        this.dateTimeTools = dateTimeTools;
        this.chatSessionService = chatSessionService;
        this.agentOrchestrator = null;
        this.legacyReactAgentExecutor = new ReactAgentExecutor(
                deepchatClient,
                ragRetrievalService,
                queryRewriteService,
                retrievalSubQueryService,
                userProfileService,
                dateTimeTools,
                scenarioPromptService,
                scenarioToolExposureService
        );
    }

    // ────── 委托给 ChatSessionService 的会话方法 ──────

    public ApiResponse<SessionCreateResponse> createSession(SessionCreateRequest request) {
        return chatSessionService.createSession(request);
    }

    public ApiResponse<SessionListResponse> getUserSessions(SessionListRequest request) {
        return chatSessionService.getUserSessions(request);
    }

    public ApiResponse<SessionDeleteResponse> deleteSession(SessionDeleteRequest request) {
        return chatSessionService.deleteSession(request);
    }

    public ApiResponse<String> extractProfile(SessionDeleteRequest request) {
        return chatSessionService.extractProfile(request);
    }

    public ApiResponse<List<Map<String, Object>>> getHistory(String userId, String sessionId) {
        return chatSessionService.getHistory(userId, sessionId);
    }

    // ────── 聊天编排 ──────

    @CircuitBreaker(name = "dashscope-chat", fallbackMethod = "chatFallback")
    @TimeLimiter(name = "dashscope-chat")
    public CompletableFuture<String> chat(String msg, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            RetrievalResult result = ragRetrievalService.retrieve(msg, userId);
            String systemPrompt = buildSingleTurnSystemPrompt(result);

            return deepchatClient.prompt()
                    .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, userId))
                    .tools(dateTimeTools)
                    .system(systemPrompt)
                    .user(msg)
                    .call()
                    .content();
        });
    }

    public CompletableFuture<String> chatFallback(String msg, String userId, Throwable t) {
        log.warn("chat 熔断降级: userId={}, error={}", userId, t.getMessage());
        return CompletableFuture.completedFuture("AI 服务暂时不可用，请稍后重试");
    }

    public Flux<ServerSentEvent<String>> multiTurnChat(MultiTurnChatRequest request) {
        if (agentOrchestrator != null) {
            return agentOrchestrator.chat(request);
        }
        String userId = chatSessionService.requireActiveSessionUser(request.getSessionId());
        if (request.getUserId() != null && !request.getUserId().equals(userId)) {
            throw new IllegalArgumentException("会话用户与当前登录用户不一致");
        }
        return legacyReactAgentExecutor.execute(request, userId);
    }

    public Flux<ServerSentEvent<String>> multiTurnChatFallback(MultiTurnChatRequest request, Throwable t) {
        log.warn("multiTurnChat 熔断降级: sessionId={}, error={}", request.getSessionId(), t.getMessage());
        ServerSentEvent<String> errorEvent = ServerSentEvent.<String>builder()
                .event("error")
                .data("AI 服务暂时不可用，请稍后重试")
                .build();
        return Flux.just(errorEvent);
    }

    private String buildSingleTurnSystemPrompt(RetrievalResult result) {
        String systemPrompt = "你是一个智能问答系统。\n"
                + "你可以使用系统提供的工具来获取实时信息。\n"
                + "当问题涉及当前时间、日期等实时数据时，请调用工具。";
        if (!result.isHit()) {
            return systemPrompt;
        }
        return systemPrompt
                + "\n\n【回答约束】\n"
                + "1. 必须优先依据【参考资料】回答，不能与引用内容相矛盾。\n"
                + "2. 如果【参考资料】中出现了用户问题相关实体或事实，不得回答“知识库未出现”“未提供相关内容”。\n"
                + "3. 如果引用内容不足以完整回答，只能说明“引用中只看到...”并列出已看到的信息，不要编造缺失部分。\n"
                + "4. 回答时尽量使用引用中的原词和结构。\n\n【参考资料】\n"
                + result.getKnowledgeText();
    }

    private String buildMultiTurnSystemPrompt(String userId, RetrievalResult result) {
        StringBuilder systemPrompt = new StringBuilder("你是一个智能问答助手。");
        String userProfile = userProfileService.getProfile(userId);
        if (userProfile != null) {
            systemPrompt.append("\n\n【用户背景与偏好（长期记忆）】\n")
                    .append(userProfile)
                    .append("\n请根据上述用户特征调整你的回答风格和内容。");
        }

        if (result.isHit()) {
            systemPrompt.append(String.format(
                    "\n\n【回答约束】\n"
                            + "1. 必须优先依据【知识库参考】回答，不能与引用内容相矛盾。\n"
                            + "2. 如果【知识库参考】中出现了用户问题相关实体或事实，不得回答“知识库未出现”“未提供相关内容”。\n"
                            + "3. 如果引用内容不足以完整回答，只能说明“引用中只看到...”并列出已看到的信息，不要编造缺失部分。\n"
                            + "4. 回答时尽量使用引用中的原词和结构。\n\n【知识库参考】\n%s",
                    result.getKnowledgeText()
            ));
        } else {
            systemPrompt.append("\n请提供专业、准确的回答。");
        }
        return systemPrompt.toString();
    }
}
