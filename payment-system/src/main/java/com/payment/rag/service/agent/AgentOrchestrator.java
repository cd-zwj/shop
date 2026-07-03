package com.payment.rag.service.agent;

import com.payment.rag.model.dto.AgentMode;
import com.payment.rag.model.dto.MultiTurnChatRequest;
import com.payment.rag.service.ChatSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AgentModeRouter agentModeRouter;
    private final AgentSessionModeService agentSessionModeService;
    private final ReactAgentExecutor reactAgentExecutor;
    private final PlanExecuteAgentExecutor planExecuteAgentExecutor;
    private final ChatSessionService chatSessionService;

    public Flux<ServerSentEvent<String>> chat(MultiTurnChatRequest request) {
        String userId = chatSessionService.requireActiveSessionUser(request.getSessionId());
        if (request.getUserId() != null && !request.getUserId().equals(userId)) {
            throw new IllegalArgumentException("会话用户与当前登录用户不一致");
        }

        AgentRouteDecision decision = resolveRouteDecision(request);
        Flux<ServerSentEvent<String>> routeFlux = Flux.just(AgentSseEvents.event("route_decision", writeJson(decision)));
        Flux<ServerSentEvent<String>> executionFlux = dispatch(decision.getMode(), request, userId);

        return Flux.concat(routeFlux, executionFlux)
                .onErrorResume(e -> {
                    log.warn("agent 编排执行失败: sessionId={}, error={}", request.getSessionId(), e.getMessage());
                    return Flux.just(AgentSseEvents.error("AI 服务暂时不可用，请稍后重试"), AgentSseEvents.done());
                });
    }

    private AgentRouteDecision resolveRouteDecision(MultiTurnChatRequest request) {
        if (request.getApprovedPlanId() != null && !request.getApprovedPlanId().isBlank()) {
            AgentRouteDecision decision = AgentRouteDecision.of(
                    AgentMode.PLAN_EXECUTE,
                    1.0,
                    "用户已确认计划，进入执行阶段",
                    "approved_plan"
            );
            agentSessionModeService.saveMode(request.getSessionId(), AgentMode.PLAN_EXECUTE, decision);
            return decision;
        }

        AgentMode modeHint = AgentMode.defaultIfNull(request.getModeHint());
        if (modeHint != AgentMode.AUTO) {
            AgentRouteDecision decision = AgentRouteDecision.of(modeHint, 1.0, "前端手动指定模式", "mode_hint");
            agentSessionModeService.saveMode(request.getSessionId(), modeHint, decision);
            return decision;
        }

        return agentSessionModeService.getMode(request.getSessionId())
                .map(mode -> AgentRouteDecision.of(mode, 1.0, "复用会话已保存模式", "session"))
                .orElseGet(() -> {
                    AgentRouteDecision decision = agentModeRouter.route(request);
                    agentSessionModeService.saveMode(request.getSessionId(), decision.getMode(), decision);
                    return decision;
                });
    }

    private Flux<ServerSentEvent<String>> dispatch(AgentMode mode, MultiTurnChatRequest request, String userId) {
        if (mode == AgentMode.PLAN_EXECUTE) {
            return planExecuteAgentExecutor.execute(request, userId);
        }
        return reactAgentExecutor.execute(request, userId);
    }

    private String writeJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("序列化路由事件失败: {}", e.getMessage());
            return "{}";
        }
    }
}
