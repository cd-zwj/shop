package com.payment.rag.service.retrieval;

import org.springframework.stereotype.Component;

/**
 * VectorStore 用户过滤表达式构建器。
 */
@Component
public class UserFilterBuilder {

    /**
     * 构建 user_id 过滤表达式。
     */
    public String build(String userId) {
        if (userId == null || userId.isBlank()) {
            return "";
        }
        return "user_id == '" + escapeFilterLiteral(userId) + "'";
    }

    private String escapeFilterLiteral(String rawValue) {
        return rawValue
                .replace("\\", "\\\\")
                .replace("'", "\\'");
    }
}
