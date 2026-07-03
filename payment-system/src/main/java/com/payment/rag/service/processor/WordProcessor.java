package com.payment.rag.service.processor;

import com.payment.rag.model.RagUnit;
import com.payment.rag.model.SourceType;
import com.payment.rag.service.MarkdownStructureChunker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * Word 文档处理器 —— 使用 MinerU 官方 API 进行高质量解析。
 * <p>
 * 适用于 .doc 和 .docx 格式，特别是含有图片、图表、表格的复杂 Word 文档。
 * MinerU 能同时处理文字与内嵌图像，输出结构化 Markdown，效果远优于 Tika 的纯文本提取。
 * <p>
 * 注意：Word 纯文本-only 的文档也可以走此处理器（MinerU 效果依然良好），
 * 但普通 Excel 等依然由 {@link TextProcessor} 使用 Tika 处理。
 */
@Service
@Slf4j
public class WordProcessor implements MediaProcessor {

    private final MinerUClient minerUClient;
    private final MarkdownStructureChunker markdownStructureChunker;

    public WordProcessor(MinerUClient minerUClient, MarkdownStructureChunker markdownStructureChunker) {
        this.minerUClient = minerUClient;
        this.markdownStructureChunker = markdownStructureChunker;
    }

    @Override
    public boolean supports(String mimeType) {
        return mimeType != null && (
                mimeType.equals("application/msword") ||                                                // .doc
                mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") // .docx
        );
    }

    /**
     * 不带 URL 的调用兜底。
     */
    @Override
    public List<RagUnit> process(InputStream input, String filename, String mimeType) {
        log.info("[WordProcessor] 使用本地 Tika 解析 Word 文档: {}", filename);
        try {
            org.apache.tika.Tika tika = new org.apache.tika.Tika();
            String rawText = tika.parseToString(input);
            if (rawText == null || rawText.isBlank()) {
                return List.of();
            }
            return markdownStructureChunker.chunk(rawText, filename, SourceType.TEXT);
        } catch (Exception e) {
            log.error("[WordProcessor] 本地 Tika 解析失败: {}", filename, e);
            throw new RuntimeException("WordProcessor 本地解析失败: " + filename, e);
        }
    }

    /**
     * 主处理方法：将 Word 文件 URL 提交给 MinerU API，获取 Markdown 文本后切分为 RagUnit。
     * 若 MinerU 未配置或解析失败，回退到本地 Tika 纯文本解析。
     *
     * @param input    文件输入流
     * @param filename 文件名
     * @param mimeType MIME 类型
     * @param fileUrl  文件的 MinIO 公网访问 URL
     */
    @Override
    public List<RagUnit> process(InputStream input, String filename, String mimeType, String fileUrl) {
        log.info("[WordProcessor] 开始处理 Word 文档: {}, URL: {}", filename, fileUrl);
        try {
            String markdownText = minerUClient.extractText(fileUrl, filename);
            if (markdownText == null || markdownText.isBlank()) {
                log.warn("[WordProcessor] MinerU 返回空结果，回退到本地 Tika 解析: {}", filename);
                return process(input, filename, mimeType);
            }
            List<RagUnit> units = markdownStructureChunker.chunk(markdownText, filename, SourceType.TEXT);
            log.info("[WordProcessor] Word 处理完成: {}，共生成 {} 个切片", filename, units.size());
            return units;
        } catch (Exception e) {
            log.warn("[WordProcessor] MinerU 解析失败，将回退到本地 Tika 解析: {}, error={}", filename, e.getMessage());
            return process(input, filename, mimeType);
        }
    }
}
