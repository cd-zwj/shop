package com.payment.rag.service.scenario;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommonScenarioTools {

    private final ScenarioBusinessTools scenarioBusinessTools;

    @Tool(description = "获取当前 AI 使用者上下文，包括角色、用户 ID、租户 ID 和权限摘要", returnDirect = false)
    public String currentActorContext() {
        return scenarioBusinessTools.currentActorContext();
    }
}
