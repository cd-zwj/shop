package com.payment.rag.service.scenario;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MerchantScenarioTools {

    private final ScenarioBusinessTools scenarioBusinessTools;

    @Tool(description = "商家端工具：读取当前商家租户订单、商品、营销、会员、财务等数据摘要。仅 merchant 角色可用", returnDirect = false)
    public String merchantDataContext() {
        return scenarioBusinessTools.merchantDataContext();
    }
}
