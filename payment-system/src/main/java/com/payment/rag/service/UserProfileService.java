package com.payment.rag.service;

import com.alibaba.cloud.ai.memory.redis.JedisRedisChatMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户画像服务（长期记忆）
 * <p>
 * 在会话结束（删除）时，异步地从对话历史中提炼用户的个人特征、偏好和事实，
 * 并以 JSON 格式持久化到 Redis 中。在后续的新对话中，将画像注入 System Prompt，
 * 使 AI 拥有跨会话的长期记忆能力。
 * <p>
 * 存储结构：
 *   Key:   user:profile:{userId}
 *   Value: JSON 字符串（由大模型生成并不断迭代更新）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileService {

    @Qualifier("redisChatMemoryRepository")
    private final JedisRedisChatMemoryRepository chatMemoryRepository;

    @Qualifier("deepchatClient")
    private final ChatClient chatClient;

    private final StringRedisTemplate redisTemplate;

    /** Redis 中存储用户画像的 Key 前缀 */
    private static final String PROFILE_KEY_PREFIX = "user:profile:";

    /** 提炼画像时，对话历史最多取多少条（防止 Token 爆炸） */
    private static final int MAX_HISTORY_FOR_EXTRACTION = 20;

    /** 助手回复截断长度（节省提炼时的 Token） */
    private static final int ASSISTANT_TRUNCATE_LENGTH = 150;

    /**
     * 提炼用户画像的 System Prompt
     * 这个 Prompt 的质量直接决定了画像的质量，需要非常精确
     */
    private static final String EXTRACTION_SYSTEM_PROMPT = """
            你是一个用户画像分析师。你的任务是从对话记录中提取关于用户的个人特征、偏好和事实。

            ## 输出规则
            1. 必须输出合法的 JSON 格式，不要包含 ```json 代码块标记，直接输出 JSON。
            2. JSON 结构如下（所有字段都是字符串数组，除了 summary 是字符串）：
               {
                 "profession": ["用户的职业或身份相关信息"],
                 "tech_stack": ["用户使用的技术、工具、框架"],
                 "preferences": ["用户的交互偏好，如回答风格、语言等"],
                 "interests": ["用户感兴趣的话题或领域"],
                 "known_facts": ["关于用户的其他具体事实"],
                 "summary": "一句话总结该用户"
               }
            3. 只提取**明确**能从对话中推断出的信息，不要猜测或编造。
            4. 如果对话中没有任何个人特征信息（比如纯粹的知识问答），返回空 JSON: {}
            5. 如果提供了旧画像，请将新发现的信息与旧画像**合并**（去重，保留最新信息，删除矛盾的旧信息）。
            """;

    /**
     * 获取用户画像文本（用于注入 System Prompt）
     *
     * @param userId 用户 ID
     * @return 画像 JSON 字符串，如果不存在则返回 null
     */
    public String getProfile(String userId) {
        String key = PROFILE_KEY_PREFIX + userId;
        String profile = redisTemplate.opsForValue().get(key);
        if (profile != null && !profile.isBlank()) {
            log.debug("获取到用户 {} 的画像: {}字", userId, profile.length());
            return profile;
        }
        return null;
    }

    /**
     * 异步提炼用户画像（推荐使用：传入预读取的对话历史）
     * <p>
     * 避免竞态问题：调用方在删除会话之前先同步读取历史消息，
     * 然后把历史消息传进来异步提炼画像，这样即使会话被删除也不影响。
     *
     * @param userId  用户 ID
     * @param history 预先读取好的对话历史消息列表
     */
    @Async("asyncTaskExecutor")
    public void extractProfileAsync(String userId, List<Message> history) {
        try {
            log.info("开始为用户 {} 提炼画像，对话消息数: {}", userId, history.size());
            long startTime = System.currentTimeMillis();

            if (history.isEmpty()) {
                log.info("对话历史为空，跳过画像提炼");
                return;
            }

            // 1. 构建对话历史文本（截断助手回复以节省 Token）
            StringBuilder conversationText = new StringBuilder();
            int count = 0;
            int startIdx = Math.max(0, history.size() - MAX_HISTORY_FOR_EXTRACTION);
            for (int i = startIdx; i < history.size(); i++) {
                Message msg = history.get(i);
                String role = msg.getMessageType() == MessageType.USER ? "用户" : "助手";
                String text = msg.getText();

                if (text == null || text.isBlank()) continue;

                // 截断过长的助手回复
                if (msg.getMessageType() == MessageType.ASSISTANT && text.length() > ASSISTANT_TRUNCATE_LENGTH) {
                    text = text.substring(0, ASSISTANT_TRUNCATE_LENGTH) + "...（已截断）";
                }

                conversationText.append(role).append("：").append(text).append("\n");
                count++;
            }

            if (count < 2) {
                log.info("有效对话不足2条，跳过画像提炼");
                return;
            }

            // 2. 获取旧画像（如果有）
            String oldProfile = getProfile(userId);
            String oldProfileSection = "";
            if (oldProfile != null) {
                oldProfileSection = "\n\n【旧画像（请在此基础上合并更新）】\n" + oldProfile;
            }

            // 3. 构建用户 Prompt
            String userPrompt = String.format("""
                    请分析以下对话，提取关于"用户"的个人特征和偏好信息。
                    %s
                    【本次对话记录】
                    %s

                    请输出合并更新后的用户画像 JSON：""",
                    oldProfileSection,
                    conversationText.toString().trim());

            // 4. 调用大模型提炼画像（独立调用，不带 Memory Advisor）
            String newProfile = chatClient.prompt()
                    .system(EXTRACTION_SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();

            // 5. 校验并保存
            if (newProfile == null || newProfile.isBlank()) {
                log.warn("画像提炼返回空结果，跳过保存");
                return;
            }

            // 去除可能的 markdown 代码块包裹
            newProfile = newProfile.trim();
            if (newProfile.startsWith("```json")) {
                newProfile = newProfile.substring(7);
            }
            if (newProfile.startsWith("```")) {
                newProfile = newProfile.substring(3);
            }
            if (newProfile.endsWith("```")) {
                newProfile = newProfile.substring(0, newProfile.length() - 3);
            }
            newProfile = newProfile.trim();

            // 如果模型返回空 JSON，说明没有个人特征
            if (newProfile.equals("{}")) {
                log.info("对话中未发现用户个人特征，保留旧画像不变");
                return;
            }

            // 保存到 Redis（永不过期，画像是长期累积的）
            String key = PROFILE_KEY_PREFIX + userId;
            redisTemplate.opsForValue().set(key, newProfile);

            long duration = System.currentTimeMillis() - startTime;
            log.info("用户 {} 画像提炼完成，耗时 {}ms，画像内容: {}", userId, duration, newProfile);

        } catch (Exception e) {
            log.error("用户 {} 画像提炼异常: {}", userId, e.getMessage(), e);
            // 画像提炼是附加功能，失败不影响主流程
        }
    }
}
