package com.payment.rag.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AI 场景枚举契约测试")
class AiScenarioTest {

    @Test
    @DisplayName("用户、商家、管理员核心场景应暴露角色范围和提示词")
    void scenarioShouldExposeRoleScopeAndPrompt() {
        assertTrue(AiScenario.USER_SHOPPING_ASSISTANT.supportsRole("user"));
        assertFalse(AiScenario.USER_SHOPPING_ASSISTANT.supportsRole("merchant"));
        assertTrue(AiScenario.USER_SHOPPING_ASSISTANT.systemPrompt().contains("用户"));

        assertTrue(AiScenario.MERCHANT_OPERATION_ASSISTANT.supportsRole("merchant"));
        assertFalse(AiScenario.MERCHANT_OPERATION_ASSISTANT.supportsRole("admin"));
        assertTrue(AiScenario.MERCHANT_OPERATION_ASSISTANT.systemPrompt().contains("商家"));

        assertTrue(AiScenario.ADMIN_GOVERNANCE_ASSISTANT.supportsRole("admin"));
        assertFalse(AiScenario.ADMIN_GOVERNANCE_ASSISTANT.supportsRole("user"));
        assertTrue(AiScenario.ADMIN_GOVERNANCE_ASSISTANT.systemPrompt().contains("管理员"));
    }

    @Test
    @DisplayName("未知场景应安全降级为通用 RAG 问答")
    void fromNullableShouldFallbackToGeneralRag() {
        assertEquals(AiScenario.GENERAL_RAG_QA, AiScenario.fromNullable(null));
        assertEquals(AiScenario.GENERAL_RAG_QA, AiScenario.fromNullable(""));
        assertEquals(AiScenario.GENERAL_RAG_QA, AiScenario.fromNullable("missing"));
    }
}