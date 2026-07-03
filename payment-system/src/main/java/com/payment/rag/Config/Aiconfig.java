package com.payment.rag.Config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankOptions;
import com.alibaba.cloud.ai.memory.redis.JedisRedisChatMemoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;

import com.alibaba.cloud.ai.model.RerankModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class Aiconfig {
    @Value("${spring.ai.dashscope.api-key}")
    private String API_KEY;

    @Value("${spring.ai.dashscope.connect-timeout-ms:10000}")
    private int connectTimeoutMs;

    @Value("${spring.ai.dashscope.read-timeout-ms:60000}")
    private int readTimeoutMs;

    @Bean
    public DashScopeApi dashScopeApi() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        return DashScopeApi.builder()
                .apiKey(API_KEY)
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .build();
    }
    @Bean(name = "qwen")
    public DashScopeChatModel dashScopeChatModel(DashScopeApi dashScopeApi) {
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen-vl-max")
                        .withMultiModel(true)
                        .build())
                .build();
    }
    @Bean(name = "deepchat")
    public DashScopeChatModel deepChatModel(DashScopeApi dashScopeApi) {
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen-plus")
                        .build())
                .build();
    }

    /**
     * 自定义聊天记忆：滑动窗口 + 累积摘要
     * 完整历史永不丢失，旧消息自动压缩为摘要注入模型上下文
     */
    @Bean
    public SummaryWindowChatMemory chatMemory(
            @Qualifier("redisChatMemoryRepository") JedisRedisChatMemoryRepository repository,
            @Qualifier("summaryChatClient") ChatClient summaryChatClient,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${chat.memory.max-messages:10}") int maxMessages,
            @Value("${chat.memory.summarize-threshold:12}") int summarizeThreshold,
            @Value("${chat.memory.assistant-truncate-length:300}") int assistantTruncateLength,
            @Value("${chat.memory.summary-max-length:500}") int summaryMaxLength) {
        if (summarizeThreshold <= maxMessages) {
            throw new IllegalArgumentException(
                    String.format("chat.memory.summarize-threshold(%d) 必须大于 chat.memory.max-messages(%d)",
                            summarizeThreshold, maxMessages));
        }
        return new SummaryWindowChatMemory(
                repository, summaryChatClient, redisTemplate, objectMapper,
                maxMessages, summarizeThreshold, assistantTruncateLength, summaryMaxLength);
    }

    @Bean
    public ChatClient chatClient(@Qualifier("qwen") DashScopeChatModel dashScopeChatModel,
                                 ChatMemory chatMemory
                                 ) {
        return ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultOptions(ChatOptions.builder().model("qwen3-omni-flash-2025-12-01").build())
                .build();
    }
    @Bean(value = "deepchatClient")
    public ChatClient deepchatClient(@Qualifier("deepchat") DashScopeChatModel dashScopeChatModel,
                                 ChatMemory chatMemory
    ) {
        return ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultOptions(ChatOptions.builder().model("qwen-plus").build())
                .build();
    }

    @Bean("summaryChatClient")
    public ChatClient summaryChatClient(@Qualifier("deepchat") ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(ChatOptions.builder().model("qwen-plus").build())
                .build();
    }

    /**
     * DashScope Rerank 模型（精排）
     * 用于对向量检索的粗召回结果进行二次精排，提升 RAG 检索质量
     */
    @Bean
    public RerankModel rerankModel(DashScopeApi dashScopeApi,
                                   @Value("${rag.rerank.model:gte-rerank-v2}") String rerankModelName) {
        return new DashScopeRerankModel(dashScopeApi,
                DashScopeRerankOptions.builder()
                        .withModel(rerankModelName)
                        .build());
    }
}
