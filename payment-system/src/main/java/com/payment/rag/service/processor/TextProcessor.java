package com.payment.rag.service.processor;

import com.payment.rag.model.RagUnit;
import com.payment.rag.model.SourceType;
import com.payment.rag.service.MarkdownStructureChunker;
import com.payment.rag.util.CharsetUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 纯文本处理器。
 * <p>
 * 当前策略：
 * - 文本文件优先走 MinerU，拿到 Markdown 后做结构化切片
 * - MinerU 失败时回退到本地原文切片，保证纯文本链路不被外部服务完全阻塞
 */
@Service
@Slf4j
public class TextProcessor implements MediaProcessor {

    private final MinerUClient minerUClient;
    private final MarkdownStructureChunker markdownStructureChunker;

    public TextProcessor(MinerUClient minerUClient, MarkdownStructureChunker markdownStructureChunker) {
        this.minerUClient = minerUClient;
        this.markdownStructureChunker = markdownStructureChunker;
    }

    @Override
    public boolean supports(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return false;
        }
        if (isCsvMimeType(mimeType)) {
            return false;
        }
        return mimeType.startsWith("text/")
                || "application/json".equals(mimeType)
                || "application/xml".equals(mimeType)
                || "application/yaml".equals(mimeType)
                || "application/x-yaml".equals(mimeType);
    }

    @Override
    public List<RagUnit> process(InputStream input, String filename, String mimeType) {
        log.info("[TextProcessor] 使用本地文本回退链路处理文件: {}", filename);
        try {
            byte[] bytes = input.readAllBytes();
            String rawText = readStringWithFallback(bytes).trim();
            if (rawText.isBlank()) {
                return List.of();
            }
            return markdownStructureChunker.chunk(rawText, filename, SourceType.TEXT);
        } catch (Exception e) {
            log.error("[TextProcessor] 本地文本切片失败: {}", filename, e);
            throw new RuntimeException("TextProcessor 处理失败: " + filename, e);
        }
    }

    @Override
    public List<RagUnit> process(InputStream input, String filename, String mimeType, String fileUrl) {
        if (filename != null && filename.toLowerCase().endsWith(".txt")) {
            log.info("[TextProcessor] 检测为 .txt 纯文本文件，直接使用本地编码自适应文本切片: {}", filename);
            return process(input, filename, mimeType);
        }
        try {
            log.info("[TextProcessor] 优先使用 MinerU 解析文本文件: {}, URL: {}", filename, fileUrl);
            String markdownText = minerUClient.extractText(fileUrl, filename);
            if (markdownText == null || markdownText.isBlank()) {
                log.warn("[TextProcessor] MinerU 返回空结果，回退到本地文本切片: {}", filename);
                return process(input, filename, mimeType);
            }
            return markdownStructureChunker.chunk(markdownText, filename, SourceType.TEXT);
        } catch (Exception e) {
            log.warn("[TextProcessor] MinerU 解析失败，回退到本地文本切片: {}, error={}", filename, e.getMessage());
            return process(input, filename, mimeType);
        }
    }

    private String readStringWithFallback(byte[] bytes) {
        return CharsetUtils.readStringWithFallback(bytes);
    }

    private boolean isCsvMimeType(String mimeType) {
        return "text/csv".equals(mimeType) || "application/csv".equals(mimeType);
    }
}
