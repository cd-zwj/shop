package com.payment.rag.service.agent;

import com.payment.rag.model.dto.AgentMode;
import com.payment.rag.model.dto.MultiTurnChatRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentModeRouter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROUTER_PROMPT = """
            你是 agent 模式路由器，只做分类，不回答用户问题。
            可选 mode 只有 REACT 或 PLAN_EXECUTE。
            规则：
            - 普通问答、解释、检索、简单聊天、当前已有 RAG 对话：REACT。
            - 需要多步骤、改代码、排查复杂问题、生成方案后执行：PLAN_EXECUTE。
            只返回 JSON：{"mode":"REACT|PLAN_EXECUTE","confidence":0.0-1.0,"reason":"一句话原因"}。
            """;

    private final ChatClient deepchatClient;

    public AgentRouteDecision route(MultiTurnChatRequest request) {
        try {
            String raw = deepchatClient.prompt()
                    .system(ROUTER_PROMPT)
                    .user(buildRouteInput(request))
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, routeConversationId(request.getSessionId())))
                    .call()
                    .content();
            return parseDecision(raw);
        } catch (Exception e) {
            log.warn("agent 路由 LLM 失败，降级 REACT: {}", e.getMessage());
            return AgentRouteDecision.reactFallback("路由失败，降级到 ReAct");
        }
    }

    AgentRouteDecision parseDecision(String raw) {
        try {
            JsonNode root = MAPPER.readTree(extractJson(raw));
            AgentMode mode = AgentMode.valueOf(root.path("mode").asText("REACT"));
            if (mode == AgentMode.AUTO) {
                mode = AgentMode.REACT;
            }
            double confidence = root.path("confidence").asDouble(0.0);
            String reason = root.path("reason").asText("路由器返回了模式判断");
            return AgentRouteDecision.of(mode, confidence, reason, "router");
        } catch (Exception e) {
            log.warn("agent 路由 JSON 非法，降级 REACT: raw={}, error={}", raw, e.getMessage());
            return AgentRouteDecision.reactFallback("路由结果非法，降级到 ReAct");
        }
    }

    private String routeConversationId(String sessionId) {
        return "agent:route:" + sessionId;
    }

    private String buildRouteInput(MultiTurnChatRequest request) {
        return "sessionId=" + request.getSessionId()
                + "\nscenario=" + request.getScenario()
                + "\nturnCount=" + request.getTurnCount()
                + "\nuserMessage=" + request.getMessage();
    }

    private String extractJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }
}
