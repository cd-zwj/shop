package com.payment.rag.service.scenario;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserScenarioTools {

    private final ScenarioBusinessTools scenarioBusinessTools;

    @Tool(description = "用户端工具：读取当前用户钱包、积分、优惠券、订单等可见数据摘要。仅 user 角色可用", returnDirect = false)
    public String userDataContext() {
        return scenarioBusinessTools.userDataContext();
    }
}
