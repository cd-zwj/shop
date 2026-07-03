package com.payment.rag.service.scenario;

import com.payment.rag.model.dto.AiScenario;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScenarioToolRegistry {

    private final List<ScenarioToolDescriptor> descriptors;

    public ScenarioToolRegistry() {
        this(defaultDescriptors());
    }

    public ScenarioToolRegistry(List<ScenarioToolDescriptor> descriptors) {
        this.descriptors = List.copyOf(descriptors == null ? List.of() : descriptors);
    }

    public List<ScenarioToolDescriptor> resolve(AiScenario scenario, String role, List<String> permissions) {
        AiScenario resolvedScenario = scenario == null ? AiScenario.GENERAL_RAG_QA : scenario;
        if (!resolvedScenario.supportsRole(role)) {
            return List.of();
        }
        return descriptors.stream()
                .filter(tool -> tool.supports(role, permissions))
                .toList();
    }

    public List<ScenarioToolDescriptor> all() {
        return descriptors;
    }

    private static List<ScenarioToolDescriptor> defaultDescriptors() {
        List<ScenarioToolDescriptor> tools = new ArrayList<>();
        tools.add(ScenarioToolDescriptor.of("current_time", "获取当前系统时间，用于回答时效性问题", List.of("user", "merchant", "admin"), List.of()));
        tools.add(ScenarioToolDescriptor.of("rag_retrieval", "执行查询改写、多路召回和 rerank 精排后的知识库检索", List.of("user", "merchant", "admin"), List.of()));
        tools.add(ScenarioToolDescriptor.of("user_wallet", "读取当前用户钱包、积分、优惠券和消费概览", List.of("user"), List.of("ai:user:wallet")));
        tools.add(ScenarioToolDescriptor.of("user_orders", "读取当前用户订单、履约和售后进度", List.of("user"), List.of("ai:user:orders")));
        tools.add(ScenarioToolDescriptor.of("merchant_orders", "读取当前商家租户的订单、履约和退款审核数据", List.of("merchant"), List.of("ai:merchant:orders")));
        tools.add(ScenarioToolDescriptor.of("merchant_marketing", "读取当前商家租户的优惠券、活动、会员等级和标签数据", List.of("merchant"), List.of("ai:merchant:marketing")));
        tools.add(ScenarioToolDescriptor.of("merchant_finance", "读取当前商家租户的钱包、提现和结算概览", List.of("merchant"), List.of("ai:merchant:finance")));
        tools.add(ScenarioToolDescriptor.of("admin_governance", "读取平台商户、用户、交易、支付、充值、提现和权限治理数据", List.of("admin"), List.of("ai:admin:governance")));
        tools.add(ScenarioToolDescriptor.of("admin_risk", "读取平台风控、退款、提现、登录安全和权限异常数据", List.of("admin"), List.of("ai:admin:risk")));
        return tools;
    }
}