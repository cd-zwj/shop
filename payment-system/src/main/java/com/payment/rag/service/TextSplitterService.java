package com.payment.rag.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文本切割服务
 * 基于 Spring AI 的 TokenTextSplitter，按 token 数量进行智能切割
 */
@Service
@Slf4j
public class TextSplitterService {

    /**
     * -- GETTER --
     *  获取当前配置的默认 chunk size
     */
    @Getter
    @Value("${chunking.token.default-chunk-size:800}")
    private int defaultChunkSize;

    @Value("${chunking.token.min-chunk-size-chars:350}")
    private int minChunkSizeChars;

    @Value("${chunking.token.min-chunk-length-to-embed:5}")
    private int minChunkLengthToEmbed;

    @Value("${chunking.token.max-num-chunks:10000}")
    private int maxNumChunks;

    @Value("${chunking.token.keep-separator:true}")
    private boolean keepSeparator;

    /**
     * 使用默认配置切割文本
     *
     * @param text 要切割的文本
     * @return 切割后的文本块列表
     */
    public List<String> splitText(String text) {
        return splitText(text, defaultChunkSize);
    }

    /**
     * 使用指定的 chunk size 切割文本
     *
     * @param text      要切割的文本
     * @param chunkSize token 数量（每个分块的大小）
     * @return 切割后的文本块列表
     */
    public List<String> splitText(String text, int chunkSize) {
        if (text == null || text.trim().isEmpty()) {
            log.debug("文本为空，返回空列表");
            return List.of();
        }

        try {
            // 创建 TokenTextSplitter
            // defaultChunkSize: 每个分块的默认 token 数量
            // minChunkSizeChars: 最小分块字符数（避免分块过小）
            // minChunkLengthToEmbed: 最小嵌入长度
            // maxNumChunks: 最大分块数量
            // keepSeparator: 是否保留分隔符（段落、句子边界）
            TokenTextSplitter splitter = new TokenTextSplitter(
                    chunkSize,
                    minChunkSizeChars,
                    minChunkLengthToEmbed,
                    maxNumChunks,
                    keepSeparator,
                    List.of()  // 使用默认标点符号
            );

            // Spring AI 的 TokenTextSplitter 需要 Document 对象
            Document document = new Document(text);

            // 执行切割
            List<Document> chunks = splitter.apply(List.of(document));

            // 提取文本内容
            List<String> result = chunks.stream()
                    .map(doc -> doc.getText())
                    .collect(Collectors.toList());

            log.info("文本已切割为 {} 个分块 (分块大小: {} token)", result.size(), chunkSize);
            return result;

        } catch (Exception e) {
            log.error("使用 TokenTextSplitter 切割文本失败，正在回退到简单切割方案", e);
            // 如果 TokenTextSplitter 失败，使用简单的备用方案
            return fallbackSplit(text, chunkSize);
        }
    }

    /**
     * 备用切割方案：简单的固定字符数切割（无重叠）
     * 当 TokenTextSplitter 失败时使用
     */
    private List<String> fallbackSplit(String text, int approximateChunkSize) {
        // 粗略估算：1 token ≈ 4 字符（对中文来说约 1.5-2 字符）
        int charChunkSize = approximateChunkSize * 3;

        List<String> chunks = new java.util.ArrayList<>();
        int length = text.length();
        int start = 0;

        while (start < length) {
            int end = Math.min(start + charChunkSize, length);
            chunks.add(text.substring(start, end));
            start = end;
        }

        log.warn("已使用兜底切割方案，生成了 {} 个分块", chunks.size());
        return chunks;
    }

}
