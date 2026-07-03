package com.payment.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class RetrievalSubQueryService {

    private static final int MAX_SUB_QUERIES = 4;

    private static final String SUB_QUERY_SYSTEM_PROMPT = """
            你是一个RAG检索查询扩展助手。你的唯一任务是：
            根据给定的主查询，生成适合知识库检索的多个简洁子查询，用于提升召回覆盖率。

            规则：
            1. 每行只输出一个检索子查询，不要编号，不要解释。
            2. 子查询必须围绕同一主题，从不同角度、别名、属性、上下游关系展开。
            3. 避免和主查询完全重复，也不要生成空泛问题。
            4. 最多输出4行。
            5. 不要回答问题，只输出检索查询。
            """;

    private final ChatClient chatClient;

    public RetrievalSubQueryService(@Qualifier("deepchatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public List<String> generateSubQueries(String rewrittenQuery, String originalQuery) {
        Set<String> queries = new LinkedHashSet<>();
        addIfPresent(queries, rewrittenQuery);
        addIfPresent(queries, originalQuery);

        if (rewrittenQuery == null || rewrittenQuery.isBlank()) {
            return new ArrayList<>(queries);
        }

        try {
            String userPrompt = String.format("""
                    【主查询】
                    %s

                    【用户原始问题】
                    %s

                    请生成最多4个用于知识库召回的检索子查询：""",
                    rewrittenQuery,
                    originalQuery == null ? "" : originalQuery);

            String content = chatClient.prompt()
                    .system(SUB_QUERY_SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();

            if (content == null || content.isBlank()) {
                return new ArrayList<>(queries);
            }

            for (String line : content.split("\\r?\\n")) {
                if (queries.size() >= MAX_SUB_QUERIES + 1) {
                    break;
                }
                addIfPresent(queries, sanitize(line));
            }
        } catch (Exception e) {
            log.warn("生成检索子查询失败，降级使用主查询与原始问题: {}", e.getMessage());
        }

        return new ArrayList<>(queries);
    }

    private void addIfPresent(Set<String> queries, String query) {
        if (query == null) {
            return;
        }
        String normalized = sanitize(query);
        if (!normalized.isBlank()) {
            queries.add(normalized);
        }
    }

    private String sanitize(String query) {
        return query == null ? "" : query
                .trim()
                .replaceFirst("^[-*0-9.\\s]+", "")
                .replaceAll("^[\"']|[\"']$", "");
    }
}
