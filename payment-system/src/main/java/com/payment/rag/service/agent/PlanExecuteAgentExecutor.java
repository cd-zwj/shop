package com.payment.rag.service.agent;

import com.payment.rag.model.dto.MultiTurnChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.payment.rag.service.scenario.ScenarioPromptService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanExecuteAgentExecutor {

    private static final String PLAN_KEY_PREFIX = "agent:plan:";
    private static final Duration PLAN_TTL = Duration.ofMinutes(30);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PLAN_PROMPT = """
            你是 Plan + 执行模式的计划生成器。
            当前阶段只能生成可审核的执行计划，不要真正执行用户任务，不要产生副作用。
            输出简洁中文 Markdown，包含目标、步骤、风险或确认点。
            """;

    private final ChatClient deepchatClient;
    private final StringRedisTemplate redisTemplate;
    private final ReactAgentExecutor reactAgentExecutor;
    private final ScenarioPromptService scenarioPromptService;

    public Flux<ServerSentEvent<String>> execute(MultiTurnChatRequest request, String userId) {
        if (request.getApprovedPlanId() != null && !request.getApprovedPlanId().isBlank()) {
            return executeApprovedPlan(request, userId);
        }
        return createPlan(request);
    }

    private Flux<ServerSentEvent<String>> createPlan(MultiTurnChatRequest request) {
        AgentPlan plan = new AgentPlan(
                UUID.randomUUID().toString(),
                request.getSessionId(),
                request.getMessage(),
                generatePlanText(request)
        );
        if (!savePlan(plan)) {
            return Flux.just(
                    AgentSseEvents.error("计划暂时无法保存，请稍后重试"),
                    AgentSseEvents.done()
            );
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("planId", plan.getPlanId());
        payload.put("mode", "PLAN_EXECUTE");
        payload.put("message", request.getMessage());
        payload.put("planText", plan.getPlanText());
        payload.put("status", "WAITING_APPROVAL");

        return Flux.just(
                AgentSseEvents.event("plan_required", writeJson(payload)),
                AgentSseEvents.done()
        );
    }

    private Flux<ServerSentEvent<String>> executeApprovedPlan(MultiTurnChatRequest request, String userId) {
        AgentPlan plan = consumePlan(request.getSessionId(), request.getApprovedPlanId());
        if (plan == null) {
            return Flux.just(
                    AgentSseEvents.error("计划不存在或已过期，请重新生成计划"),
                    AgentSseEvents.done()
            );
        }
        if (!plan.getSessionId().equals(request.getSessionId())) {
            return Flux.just(
                    AgentSseEvents.error("计划与当前会话不匹配"),
                    AgentSseEvents.done()
            );
        }

        MultiTurnChatRequest executeRequest = new MultiTurnChatRequest(
                request.getUserId(),
                request.getSessionId(),
                request.getTurnCount(),
                buildExecutionMessage(request, plan),
                request.getScenario(),
                request.getModeHint(),
                request.getApprovedPlanId()
        );
        return reactAgentExecutor.execute(executeRequest, userId);
    }

    private String generatePlanText(MultiTurnChatRequest request) {
        try {
            return deepchatClient.prompt()
                    .system(scenarioPromptService.buildScenarioPrompt(request.getScenario()) + "\n\n" + PLAN_PROMPT)
                    .user(request.getMessage())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, planConversationId(request.getSessionId())))
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("生成计划失败，使用本地兜底计划: {}", e.getMessage());
            return """
                    ## 执行计划
                    1. 明确需求范围和预期结果。
                    2. 检查现有上下文与相关资料。
                    3. 按最小可验证步骤执行。
                    4. 汇总结果、风险和后续建议。
                    """;
        }
    }

    private String planConversationId(String sessionId) {
        return "agent:plan:" + sessionId;
    }

    private String buildExecutionMessage(MultiTurnChatRequest request, AgentPlan plan) {
        String userMessage = request.getMessage();
        if (userMessage == null || userMessage.isBlank()) {
            userMessage = plan.getOriginalMessage();
        }
        return userMessage
                + "\n\n【用户已确认执行以下计划】\n"
                + plan.getPlanText();
    }

    private boolean savePlan(AgentPlan plan) {
        try {
            redisTemplate.opsForValue().set(planKey(plan.getSessionId(), plan.getPlanId()), MAPPER.writeValueAsString(plan), PLAN_TTL);
            return true;
        } catch (Exception e) {
            log.warn("保存计划到 Redis 失败: planId={}, error={}", plan.getPlanId(), e.getMessage());
            return false;
        }
    }

    private AgentPlan consumePlan(String sessionId, String planId) {
        try {
            String raw = redisTemplate.opsForValue().getAndDelete(planKey(sessionId, planId));
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return MAPPER.readValue(raw, AgentPlan.class);
        } catch (Exception e) {
            log.warn("消费计划失败: planId={}, error={}", planId, e.getMessage());
            return null;
        }
    }

    private String writeJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("序列化计划事件失败: {}", e.getMessage());
            return "{}";
        }
    }

    private String planKey(String sessionId, String planId) {
        return PLAN_KEY_PREFIX + sessionId + ":" + planId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class AgentPlan {
        private String planId;
        private String sessionId;
        private String originalMessage;
        private String planText;
    }
}
