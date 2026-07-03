package com.payment.rag.service.scenario;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminScenarioTools {

    private final ScenarioBusinessTools scenarioBusinessTools;

    @Tool(description = "管理员工具：读取平台治理、商户、用户、交易、提现、权限、风控等数据摘要。仅 admin 角色可用", returnDirect = false)
    public String adminDataContext() {
        return scenarioBusinessTools.adminDataContext();
    }
}
