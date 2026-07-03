package com.payment.rag.service.processor;

import com.payment.rag.model.RagUnit;
import com.payment.rag.model.SourceType;
import com.payment.rag.service.MarkdownStructureChunker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * PDF 文件处理器 —— 使用 MinerU 官方 API 进行高质量解析。
 * <p>
 * MinerU 相较于 Apache Tika 的优势：
 * - 保留公式、表格结构（输出为 Markdown）
 * - 对扫描件/图文混合 PDF 具有 OCR 能力
 * - 输出结构化 Markdown，有利于后续 RAG 切片
 * <p>
 * 由于 mineru.net 官方 API 为异步任务模式（提交 URL → 等待解析 → 取结果），
 * 此 Processor 需要文件已上传至 MinIO 并可通过公网 URL 访问。
 * 因此本类重写了带 fileUrl 参数的 {@link MediaProcessor#process(InputStream, String, String, String)} 方法。
 */
@Service
@Slf4j
public class PdfProcessor implements MediaProcessor {

    private final MinerUClient minerUClient;
    private final MarkdownStructureChunker markdownStructureChunker;

    public PdfProcessor(MinerUClient minerUClient, MarkdownStructureChunker markdownStructureChunker) {
        this.minerUClient = minerUClient;
        this.markdownStructureChunker = markdownStructureChunker;
    }

    @Override
    public boolean supports(String mimeType) {
        return "application/pdf".equals(mimeType);
    }

    /**
     * 不带 URL 的调用（理论上不应发生，兜底警告日志）。
     * FileProcessConsumer 始终会传入 fileUrl，此方法仅作保险。
     */
    @Override
    public List<RagUnit> process(InputStream input, String filename, String mimeType) {
        log.info("[PdfProcessor] 使用本地 Tika 解析 PDF 文档: {}", filename);
        try {
            org.apache.tika.Tika tika = new org.apache.tika.Tika();
            String rawText = tika.parseToString(input);
            if (rawText == null || rawText.isBlank()) {
                return List.of();
            }
            return markdownStructureChunker.chunk(rawText, filename, SourceType.TEXT);
        } catch (Exception e) {
            log.error("[PdfProcessor] 本地 Tika 解析失败: {}", filename, e);
            throw new RuntimeException("PdfProcessor 本地解析失败: " + filename, e);
        }
    }

    /**
     * 主处理方法：将文件 URL 提交给 MinerU API，获取 Markdown 文本后切分为 RagUnit。
     * 若 MinerU 未配置或解析失败，回退到本地 Tika 纯文本解析。
     *
     * @param input    文件输入流
     * @param filename 文件名
     * @param mimeType MIME 类型
     * @param fileUrl  文件的 MinIO 公网访问 URL（MinerU 需要从此 URL 拉取文件）
     */
    @Override
    public List<RagUnit> process(InputStream input, String filename, String mimeType, String fileUrl) {
        log.info("[PdfProcessor] 开始处理 PDF: {}, URL: {}", filename, fileUrl);
        try {
            String markdownText = minerUClient.extractText(fileUrl, filename);
            if (markdownText == null || markdownText.isBlank()) {
                log.warn("[PdfProcessor] MinerU 返回空结果，回退到本地 Tika 解析: {}", filename);
                return process(input, filename, mimeType);
            }
            List<RagUnit> units = markdownStructureChunker.chunk(markdownText, filename, SourceType.TEXT);
            log.info("[PdfProcessor] PDF 处理完成: {}，共生成 {} 个切片", filename, units.size());
            return units;
        } catch (Exception e) {
            log.warn("[PdfProcessor] MinerU 解析失败，将回退到本地 Tika 解析: {}, error={}", filename, e.getMessage());
            return process(input, filename, mimeType);
        }
    }
}
