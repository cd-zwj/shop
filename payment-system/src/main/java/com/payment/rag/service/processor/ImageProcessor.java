package com.payment.rag.service.processor;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.payment.rag.model.RagUnit;
import com.payment.rag.model.SourceType;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ImageProcessor implements MediaProcessor {

    private final ChatClient chatClient;

    public ImageProcessor(@Qualifier("qwen") ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public boolean supports(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    @Override
    public List<RagUnit> process(InputStream input, String filename, String mimeType) {
        log.warn("ImageProcessor.process 被调用，但未提供 URL，使用文件名作为描述");
        return createFallbackUnit(filename);
    }

    @Override
    public List<RagUnit> process(InputStream input, String filename, String mimeType, String fileUrl) {
        log.info("正在处理图片文件: {}, URL: {}", filename, fileUrl);
        List<RagUnit> units = new ArrayList<>();

        try {
            String description = describeImageByUrl(fileUrl, mimeType);

            if (description == null || description.trim().isEmpty()) {
                description = "图片文件: " + filename + " (AI描述生成失败)";
            }

            RagUnit unit = new RagUnit();
            unit.setSourceType(SourceType.IMAGE);
            unit.setContent(description.trim());
            unit.setChunkIndex(0);
            unit.setSourceId(UUID.randomUUID().toString());
            units.add(unit);

        } catch (Exception e) {
            log.error("处理图片文件时出错: {}", filename, e);
            return createFallbackUnit(filename);
        }

        return units;
    }

    private List<RagUnit> createFallbackUnit(String filename) {
        List<RagUnit> units = new ArrayList<>();
        RagUnit unit = new RagUnit();
        unit.setSourceType(SourceType.IMAGE);
        unit.setContent("图片文件: " + filename);
        unit.setChunkIndex(0);
        unit.setSourceId(UUID.randomUUID().toString());
        units.add(unit);
        return units;
    }

    /**
     * 通过 URL 调用多模态 AI 生成图片描述
     */
    public String describeImageByUrl(String imageUrl, String mimeType) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                // 1. 创建 Media 对象
                List<Media> mediaList = List.of(new Media(MimeTypeUtils.IMAGE_PNG,
                        new URI(imageUrl).toURL().toURI()));

                // 2. 构建 UserMessage
                String prompt = "请详细描述这张图片的内容，包括：1. 图片中的主要对象和场景；2. 图片中出现的任何文字；3. 图片的整体含义或用途。请用中文回答。";
                UserMessage message = UserMessage.builder()
                        .text(prompt)
                        .media(mediaList)
                        .metadata(new HashMap<>())
                        .build();
                // 设置消息格式为图片模式
                message.getMetadata().put("message_format", "image");

                // 3. 使用 DashScopeChatOptions 配置多模态
                ChatResponse response = chatClient
                        .prompt(new Prompt(message,
                                DashScopeChatOptions.builder()
                                        .withModel("qwen-vl-max")
                                        .withMultiModel(true)
                                        .build()))
                        .call()
                        .chatResponse();

                String result = response.getResult().getOutput().getText();
                log.info("AI 图片描述生成成功");
                return result;

            } catch (Exception e) {
                log.warn("AI 图片描述生成尝试 {}/{} 失败: {}", i + 1, maxRetries, e.getMessage());
                if (i == maxRetries - 1) {
                    log.error("AI 图片描述生成最终失败, URL: {}", imageUrl, e);
                    return "";
                }
                try {
                    Thread.sleep(1000); // 失败后等待1秒重试
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return "";
                }
            }
        }
        return "";
    }
}
