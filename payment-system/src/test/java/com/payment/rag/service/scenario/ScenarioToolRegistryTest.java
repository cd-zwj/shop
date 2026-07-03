package com.payment.rag.service.scenario;

import com.payment.rag.model.dto.AiScenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AI 场景工具权限注册表测试")
class ScenarioToolRegistryTest {

    @Test
    @DisplayName("用户场景只能获得用户和公共工具")
    void userScenarioShouldOnlyExposeUserTools() {
        ScenarioToolRegistry registry = new ScenarioToolRegistry(List.of(
                ScenarioToolDescriptor.of("current_time", "公共时间", List.of("user", "merchant", "admin"), List.of()),
                ScenarioToolDescriptor.of("user_wallet", "用户钱包", List.of("user"), List.of("ai:user:wallet")),
                ScenarioToolDescriptor.of("merchant_orders", "商家订单", List.of("merchant"), List.of("ai:merchant:orders")),
                ScenarioToolDescriptor.of("admin_risk", "管理风控", List.of("admin"), List.of("ai:admin:risk"))
        ));

        List<String> toolNames = registry.resolve(AiScenario.USER_WALLET_ADVISOR, "user", List.of("ai:user:wallet"))
                .stream()
                .map(ScenarioToolDescriptor::name)
                .toList();

        assertTrue(toolNames.contains("current_time"));
        assertTrue(toolNames.contains("user_wallet"));
        assertFalse(toolNames.contains("merchant_orders"));
        assertFalse(toolNames.contains("admin_risk"));
    }

    @Test
    @DisplayName("缺少权限时同角色工具也不能暴露")
    void missingPermissionShouldHideRoleTool() {
        ScenarioToolRegistry registry = new ScenarioToolRegistry(List.of(
                ScenarioToolDescriptor.of("merchant_orders", "商家订单", List.of("merchant"), List.of("ai:merchant:orders"))
        ));

        assertTrue(registry.resolve(AiScenario.MERCHANT_ORDER_ASSISTANT, "merchant", List.of()).isEmpty());
        assertEquals(1, registry.resolve(AiScenario.MERCHANT_ORDER_ASSISTANT, "merchant", List.of("ai:merchant:orders")).size());
    }
}