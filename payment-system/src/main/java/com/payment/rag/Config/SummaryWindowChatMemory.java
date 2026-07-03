package com.payment.rag.Config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 滑动窗口 + 累积摘要的聊天记忆实现。
 * <p>
 * 三层存储：
 * <ul>
 *   <li>完整历史 → Redis List: session:full-history:{conversationId}（永不裁剪）</li>
 *   <li>会话摘要 → Redis String: session:summary:{conversationId}（旧消息压缩）</li>
 *   <li>窗口消息 → ChatMemoryRepository（仅保留最近 maxMessages 条，供模型上下文使用）</li>
 * </ul>
 * <p>
 * {@link #get(String)} 返回：摘要 SystemMessage + 窗口内最近 N 条原始消息，
 * 保证模型同时看到早期要点和近期细节。
 */
@Slf4j
public class SummaryWindowChatMemory implements ChatMemory {

    private static final String FULL_HISTORY_PREFIX = "session:full-history:";
    private static final String SUMMARY_PREFIX = "session:summary:";

    private static final String SUMMARY_SYSTEM_PROMPT = """
            你是一个对话摘要助手。请将以下对话压缩为结构化摘要。

            ## 输出格式（纯文本，不要 JSON，不要 markdown 代码块）：
            【用户目标】用户想要做什么
            【已确认事实】已确认的关键事实
            【关键结论】已得出的结论
            【未解决问题】尚未解决的问题
            【用户偏好/约束】用户的偏好或限制条件

            ## 规则：
            1. 只保留对后续对话有用的信息，去掉寒暄和重复
            2. 如果提供了旧摘要，请将新旧信息合并去重
            3. 控制在 %d 字以内
            """;

    private final ChatMemoryRepository repository;
    private final ChatClient summaryChatClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int maxMessages;
    private final int summarizeThreshold;
    private final int assistantTruncateLength;
    private final int summaryMaxLength;

    public SummaryWindowChatMemory(ChatMemoryRepository repository,
                                   ChatClient summaryChatClient,
                                   StringRedisTemplate redisTemplate,
                                   ObjectMapper objectMapper,
                                   int maxMessages,
                                   int summarizeThreshold,
                                   int assistantTruncateLength,
                                   int summaryMaxLength) {
        this.repository = repository;
        this.summaryChatClient = summaryChatClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.maxMessages = maxMessages;
        this.summarizeThreshold = summarizeThreshold;
        this.assistantTruncateLength = assistantTruncateLength;
        this.summaryMaxLength = summaryMaxLength;
    }

    // ==================== ChatMemory 接口实现 ====================

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        // 1. 追加到完整历史（永不丢失）
        appendToFullHistory(conversationId, messages);

        // 2. 判断是否需要摘要
        long fullCount = getFullHistoryCount(conversationId);
        if (fullCount > summarizeThreshold) {
            generateSummaryIfNeeded(conversationId);
        }

        // 3. 保存窗口内消息到 Repository（框架 advisor 从此读取）
        List<Message> fullHistory = loadFullHistoryAsMessages(conversationId);
        List<Message> window = extractWindow(fullHistory);
        repository.saveAll(conversationId, window);
    }

    @Override
    public List<Message> get(String conversationId) {
        List<Message> windowMessages = repository.findByConversationId(conversationId);
        if (windowMessages == null) {
            windowMessages = List.of();
        }
        String summary = getSummaryText(conversationId);

        if (summary != null && !summary.isBlank()) {
            List<Message> result = new ArrayList<>();
            result.add(new SystemMessage("【会话摘要（早期对话要点）】\n" + summary
                    + "\n请结合上述摘要理解用户当前问题的上下文。"));
            result.addAll(windowMessages);
            return result;
        }

        return windowMessages;
    }

    @Override
    public void clear(String conversationId) {
        repository.deleteByConversationId(conversationId);
        redisTemplate.delete(FULL_HISTORY_PREFIX + conversationId);
        redisTemplate.delete(SUMMARY_PREFIX + conversationId);
        log.info("会话 {} 的所有记忆已清理（窗口+完整历史+摘要）", conversationId);
    }

    // ==================== 完整历史读取（供外部使用） ====================

    /**
     * 获取完整历史消息列表（供画像提炼、查询改写等使用）
     */
    public List<Message> getFullHistory(String conversationId) {
        return loadFullHistoryAsMessages(conversationId);
    }

    /**
     * 获取完整历史的详细信息（带时间戳，供前端展示）
     */
    public List<Map<String, Object>> getFullHistoryDetail(String conversationId) {
        List<String> raw = redisTemplate.opsForList().range(FULL_HISTORY_PREFIX + conversationId, 0, -1);
        List<Map<String, Object>> result = new ArrayList<>();
        if (raw == null) {
            return result;
        }
        for (String json : raw) {
            try {
                Map<String, Object> item = objectMapper.readValue(json, new TypeReference<>() {});
                result.add(item);
            } catch (JsonProcessingException e) {
                log.warn("解析完整历史消息失败: {}", e.getMessage());
            }
        }
        return result;
    }

    /**
     * 获取会话摘要文本（从 JSON 中提取 summary 字段）
     */
    public String getSummaryText(String conversationId) {
        String raw = redisTemplate.opsForValue().get(SUMMARY_PREFIX + conversationId);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> data = objectMapper.readValue(raw, new TypeReference<>() {});
            Object summary = data.get("summary");
            return summary != null ? summary.toString() : null;
        } catch (JsonProcessingException e) {
            // 兼容非 JSON 格式（如旧数据或手动写入的纯文本）
            return raw;
        }
    }

    /**
     * 获取已摘要覆盖的消息条数
     */
    private int getSummaryCount(String conversationId) {
        String raw = redisTemplate.opsForValue().get(SUMMARY_PREFIX + conversationId);
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            Map<String, Object> data = objectMapper.readValue(raw, new TypeReference<>() {});
            Object count = data.get("count");
            return count instanceof Number ? ((Number) count).intValue() : 0;
        } catch (JsonProcessingException e) {
            return 0;
        }
    }

    private void saveSummaryData(String conversationId, String summaryText, int count) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "summary", summaryText,
                    "count", count
            ));
            redisTemplate.opsForValue().set(SUMMARY_PREFIX + conversationId, json);
        } catch (JsonProcessingException e) {
            log.error("序列化摘要数据失败: {}", e.getMessage());
        }
    }

    /**
     * 获取完整历史消息数量
     */
    public long getFullHistoryCount(String conversationId) {
        Long count = redisTemplate.opsForList().size(FULL_HISTORY_PREFIX + conversationId);
        return count != null ? count : 0;
    }

    // ==================== 完整历史写入 ====================

    private void appendToFullHistory(String conversationId, List<Message> messages) {
        String key = FULL_HISTORY_PREFIX + conversationId;
        for (Message msg : messages) {
            try {
                String json = serializeMessage(msg);
                redisTemplate.opsForList().rightPush(key, json);
            } catch (JsonProcessingException e) {
                log.error("序列化消息失败，跳过该条: {}", e.getMessage());
            }
        }
    }

    // ==================== 摘要生成 ====================

    private void generateSummaryIfNeeded(String conversationId) {
        try {
            List<Message> fullHistory = loadFullHistoryAsMessages(conversationId);
            if (fullHistory.size() <= summarizeThreshold) {
                return;
            }

            // 窗口边界：窗口内保留的消息不参与摘要
            int windowBoundary = fullHistory.size() - maxMessages;
            if (windowBoundary <= 0) {
                return;
            }

            // 增量：只取上次摘要之后新进入"窗口外"的消息
            int lastCount = getSummaryCount(conversationId);
            int newStart = Math.max(0, Math.min(lastCount, windowBoundary));
            if (newStart >= windowBoundary) {
                // 没有新消息需要摘要
                return;
            }
            List<Message> newMessages = fullHistory.subList(newStart, windowBoundary);

            String oldSummary = getSummaryText(conversationId);
            String newSummary = generateSummary(oldSummary, newMessages);

            if (newSummary != null && !newSummary.isBlank()) {
                saveSummaryData(conversationId, newSummary, windowBoundary);
                log.info("会话 {} 摘要增量更新，新增摘要消息数: {}，已覆盖: {}，摘要长度: {}",
                        conversationId, newMessages.size(), windowBoundary, newSummary.length());
            }
        } catch (Exception e) {
            log.error("会话 {} 摘要生成异常: {}", conversationId, e.getMessage(), e);
        }
    }

    private String generateSummary(String oldSummary, List<Message> toSummarize) {
        StringBuilder conversationText = new StringBuilder();
        for (Message msg : toSummarize) {
            String role = msg.getMessageType() == MessageType.USER ? "用户" : "助手";
            String text = msg.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            if (msg.getMessageType() == MessageType.ASSISTANT && text.length() > assistantTruncateLength) {
                text = text.substring(0, assistantTruncateLength) + "...（已截断）";
            }
            conversationText.append(role).append("：").append(text).append("\n");
        }

        if (conversationText.isEmpty()) {
            return null;
        }

        String userPrompt;
        if (oldSummary != null && !oldSummary.isBlank()) {
            userPrompt = String.format("""
                    【旧摘要（请在此基础上合并更新）】
                    %s

                    【需要摘要的新对话】
                    %s

                    请输出合并更新后的会话摘要：""",
                    oldSummary, conversationText.toString().trim());
        } else {
            userPrompt = String.format("""
                    【需要摘要的对话】
                    %s

                    请输出会话摘要：""",
                    conversationText.toString().trim());
        }

        try {
            String result = summaryChatClient.prompt()
                    .system(String.format(SUMMARY_SYSTEM_PROMPT, summaryMaxLength))
                    .user(userPrompt)
                    .call()
                    .content();

            return cleanSummaryResult(result);
        } catch (Exception e) {
            log.error("调用 LLM 生成摘要失败: {}", e.getMessage());
            return null;
        }
    }

    private String cleanSummaryResult(String result) {
        if (result == null || result.isBlank()) {
            return null;
        }
        String cleaned = result.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    // ==================== 窗口截取 ====================

    private List<Message> extractWindow(List<Message> fullHistory) {
        if (fullHistory.size() <= maxMessages) {
            return new ArrayList<>(fullHistory);
        }
        return new ArrayList<>(fullHistory.subList(fullHistory.size() - maxMessages, fullHistory.size()));
    }

    // ==================== 序列化 / 反序列化 ====================

    private String serializeMessage(Message msg) throws JsonProcessingException {
        return objectMapper.writeValueAsString(Map.of(
                "type", msg.getMessageType().getValue(),
                "content", msg.getText() != null ? msg.getText() : "",
                "timestamp", System.currentTimeMillis()
        ));
    }

    private List<Message> loadFullHistoryAsMessages(String conversationId) {
        String key = FULL_HISTORY_PREFIX + conversationId;
        List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
        List<Message> messages = new ArrayList<>();
        if (raw == null) {
            return messages;
        }
        for (String json : raw) {
            try {
                Map<String, Object> item = objectMapper.readValue(json, new TypeReference<>() {});
                String type = (String) item.get("type");
                String content = (String) item.get("content");
                if (content == null || content.isBlank()) {
                    continue;
                }
                messages.add(switchToMessage(type, content));
            } catch (JsonProcessingException e) {
                log.warn("反序列化消息失败: {}", e.getMessage());
            }
        }
        return messages;
    }

    private Message switchToMessage(String type, String content) {
        return switch (type != null ? type.toLowerCase() : "") {
            case "user" -> new UserMessage(content);
            case "system" -> new SystemMessage(content);
            default -> new AssistantMessage(content);
        };
    }
}
