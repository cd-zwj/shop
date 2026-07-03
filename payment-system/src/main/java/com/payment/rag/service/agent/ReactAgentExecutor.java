package com.payment.rag.service.agent;

import com.payment.rag.Config.DateTimeTools;
import com.payment.rag.model.dto.HierarchyHit;
import com.payment.rag.model.dto.MultiTurnChatRequest;
import com.payment.rag.model.dto.RetrievalResult;
import com.payment.rag.service.QueryRewriteService;
import com.payment.rag.service.RagRetrievalService;
import com.payment.rag.service.RetrievalSubQueryService;
import com.payment.rag.service.UserProfileService;
import com.payment.rag.service.scenario.ScenarioPromptService;
import com.payment.rag.service.scenario.ScenarioToolExposureService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactAgentExecutor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChatClient deepchatClient;
    private final RagRetrievalService ragRetrievalService;
    private final QueryRewriteService queryRewriteService;
    private final RetrievalSubQueryService retrievalSubQueryService;
    private final UserProfileService userProfileService;
    private final DateTimeTools dateTimeTools;
    private final ScenarioPromptService scenarioPromptService;
    private final ScenarioToolExposureService scenarioToolExposureService;

    public Flux<ServerSentEvent<String>> execute(MultiTurnChatRequest request, String userId) {
        String originalQuery = request.getMessage();
        String rewrittenQuery = queryRewriteService.rewrite(request.getSessionId(), originalQuery);
        log.info("ReAct 多轮对话检索 - 原始查询: '{}', 改写后: '{}'", originalQuery, rewrittenQuery);

        List<String> retrievalQueries = retrievalSubQueryService.generateSubQueries(rewrittenQuery, originalQuery);
        RetrievalResult result = ragRetrievalService.retrieveWithMultiPathRecall(
                rewrittenQuery,
                retrievalQueries,
                userId
        );

        String systemPrompt = scenarioPromptService.buildScenarioPrompt(request.getScenario())
                + "\n\n"
                + buildMultiTurnSystemPrompt(userId, result);
        ServerSentEvent<String> citationsEvent = AgentSseEvents.event("citations", serializeCitations(result));

        Flux<ServerSentEvent<String>> textFlux = deepchatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, request.getSessionId()))
                .tools(scenarioToolExposureService.exposedTools(request.getScenario(), dateTimeTools))
                .system(systemPrompt)
                .user(originalQuery)
                .stream()
                .content()
                .map(token -> AgentSseEvents.event("token", token));

        return Flux.concat(Flux.just(citationsEvent), textFlux, Flux.just(AgentSseEvents.done()));
    }

    private String serializeCitations(RetrievalResult result) {
        List<Map<String, Object>> citations = new ArrayList<>();
        if (result.getHierarchyHits() != null) {
            for (int i = 0; i < result.getHierarchyHits().size(); i++) {
                HierarchyHit hit = result.getHierarchyHits().get(i);
                Map<String, Object> cite = new HashMap<>();
                cite.put("sourceName", hit.getFilename() != null ? hit.getFilename() : "");
                cite.put("minioUrl", hit.getMinioUrl() != null ? hit.getMinioUrl() : "");
                cite.put("docTitle", hit.getDocTitle() != null ? hit.getDocTitle() : "");
                cite.put("sectionTitle", hit.getSectionTitle() != null ? hit.getSectionTitle() : "");
                cite.put("chunkIndex", hit.getLeafChunkIndex() != null ? hit.getLeafChunkIndex() + 1 : null);

                String label;
                if (hit.getSectionTitle() != null && !hit.getSectionTitle().isBlank()) {
                    label = hit.getSectionTitle();
                } else if (hit.getLeafChunkIndex() != null) {
                    label = "分段 " + (hit.getLeafChunkIndex() + 1);
                } else {
                    label = "段落 " + (i + 1);
                }
                cite.put("label", label);
                cite.put("text", hit.getContent() != null ? hit.getContent() : "");

                double scoreDouble = hit.getLeafScore() != null ? hit.getLeafScore() : 0.0;
                int scorePercent = (int) Math.round(scoreDouble * 100);
                cite.put("score", scorePercent);

                citations.add(cite);
            }
        }

        try {
            return MAPPER.writeValueAsString(citations);
        } catch (Exception e) {
            log.error("序列化引文失败", e);
            return "[]";
        }
    }

    private String buildMultiTurnSystemPrompt(String userId, RetrievalResult result) {
        StringBuilder systemPrompt = new StringBuilder("你是一个智能问答助手。");
        String userProfile = userProfileService.getProfile(userId);
        if (userProfile != null) {
            systemPrompt.append("\n\n【用户背景与偏好（长期记忆）】\n")
                    .append(userProfile)
                    .append("\n请根据上述用户特征调整你的回答风格和内容。");
        }

        if (result.isHit()) {
            systemPrompt.append(String.format(
                    "\n\n【回答约束】\n"
                            + "1. 必须优先依据【知识库参考】回答，不能与引用内容相矛盾。\n"
                            + "2. 如果【知识库参考】中出现了用户问题相关实体或事实，不得回答“知识库未出现”“未提供相关内容”。\n"
                            + "3. 如果引用内容不足以完整回答，只能说明“引用中只看到...”并列出已看到的信息，不要编造缺失部分。\n"
                            + "4. 回答时尽量使用引用中的原词和结构。\n\n【知识库参考】\n%s",
                    result.getKnowledgeText()
            ));
        } else {
            systemPrompt.append("\n请提供专业、准确的回答。");
        }
        return systemPrompt.toString();
    }
}
