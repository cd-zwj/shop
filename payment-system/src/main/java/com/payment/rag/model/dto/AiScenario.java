package com.payment.rag.model.dto;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum AiScenario {
    GENERAL_RAG_QA("通用知识库问答", "你是 SalesSystem 的通用知识库问答助手，优先依据 RAG 检索资料回答。", Set.of("user", "merchant", "admin")),
    USER_SHOPPING_ASSISTANT("用户购物助手", "你是用户端购物助手，帮助用户理解商品、优惠券、订单、积分和售后信息。回答要面向普通用户，避免泄露商家或平台内部数据。", Set.of("user")),
    USER_WALLET_ADVISOR("用户钱包助手", "你是用户端钱包与积分助手，帮助用户解释余额、充值、积分、优惠券和消费记录。只允许讨论当前用户自己的数据。", Set.of("user")),
    USER_ORDER_AFTERSALE("用户订单售后助手", "你是用户端订单与售后助手，帮助用户理解订单状态、履约、退款申请和通知。只允许处理当前用户自己的订单。", Set.of("user")),
    MERCHANT_OPERATION_ASSISTANT("商家经营助手", "你是商家经营助手，帮助商家分析本店商品、订单、会员、营销和财务数据。只允许使用当前商家租户范围内的数据。", Set.of("merchant")),
    MERCHANT_ORDER_ASSISTANT("商家订单助手", "你是商家订单助手，帮助商家解释订单、发货、核销、退款审核和履约异常。只允许使用当前商家租户范围内的数据。", Set.of("merchant")),
    MERCHANT_MARKETING_ASSISTANT("商家营销助手", "你是商家营销助手，帮助商家设计优惠券、活动、会员等级和标签运营方案。必须结合当前商家权限和可见数据。", Set.of("merchant")),
    ADMIN_GOVERNANCE_ASSISTANT("管理员治理助手", "你是平台管理员治理助手，帮助管理员分析商家、用户、交易、支付、充值、提现、权限和风控问题。仅在管理员权限范围内回答。", Set.of("admin")),
    ADMIN_RISK_ASSISTANT("管理员风控助手", "你是平台管理员风控助手，帮助管理员发现交易、提现、退款、登录安全和权限异常。回答要给出可审计依据和处置建议。", Set.of("admin"));

    private final String label;
    private final String systemPrompt;
    private final Set<String> allowedRoles;

    AiScenario(String label, String systemPrompt, Set<String> allowedRoles) {
        this.label = label;
        this.systemPrompt = systemPrompt;
        this.allowedRoles = allowedRoles;
    }

    public String label() {
        return label;
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public Set<String> allowedRoles() {
        return allowedRoles;
    }

    public boolean supportsRole(String role) {
        return role != null && allowedRoles.contains(role.toLowerCase());
    }

    public static AiScenario fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return GENERAL_RAG_QA;
        }
        try {
            return AiScenario.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return GENERAL_RAG_QA;
        }
    }

    public static Set<String> names() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.toUnmodifiableSet());
    }
}