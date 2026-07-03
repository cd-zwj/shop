package com.payment.rag.service.scenario;

import com.payment.config.AuthStpKit;
import com.payment.rag.model.dto.AiScenario;
import com.payment.rag.service.AuthContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScenarioPromptService {

    private final AuthContextService authContextService;
    private final ScenarioToolRegistry scenarioToolRegistry;

    public String buildScenarioPrompt(AiScenario scenario) {
        AiScenario resolved = scenario == null ? AiScenario.GENERAL_RAG_QA : scenario;
        String role = authContextService.getCurrentRole();
        List<String> permissions = authContextService.getCurrentPermissions();
        if (!resolved.supportsRole(role)) {
            throw new IllegalArgumentException("当前角色无权使用 AI 场景: " + resolved.name());
        }
        List<ScenarioToolDescriptor> tools = scenarioToolRegistry.resolve(resolved, role, permissions);
        String toolText = tools.isEmpty()
                ? "无可用业务工具，仅允许基于 RAG 引用和通用推理回答。"
                : tools.stream()
                .map(tool -> "- " + tool.name() + ": " + tool.description())
                .collect(Collectors.joining("\n"));

        return "【当前 AI 场景】" + resolved.label()
                + "\n【角色边界】当前使用者角色为 " + role + "，只能访问该角色和权限范围内的数据。"
                + "\n【场景提示词】\n" + resolved.systemPrompt()
                + "\n\n【动态可用工具】\n" + toolText
                + "\n\n【安全要求】不得要求、展示或推断用户无权访问的数据；当工具或 RAG 引用不足时，明确说明缺少依据。";
    }

    public String currentRoleForTelemetry() {
        if (!(AuthStpKit.PLATFORM.isLogin() || AuthStpKit.MERCHANT.isLogin() || AuthStpKit.ADMIN.isLogin())) {
            return "anonymous";
        }
        return authContextService.getCurrentRole();
    }
}