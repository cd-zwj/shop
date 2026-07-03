package com.payment.rag.service.retrieval;

import com.payment.rag.model.RagUnit;
import com.payment.rag.repository.RagUnitQueryRepository;
import com.payment.rag.service.VectorStoreWriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识文本构建器，负责将检索命中的文档和节点格式化为可注入 System Prompt 的文本。
 * <p>
 * 从 RagRetrievalService 中抽取的共享工具方法。
 */
@Component
@RequiredArgsConstructor
public class KnowledgeTextBuilder {

    private static final int CONTEXT_NEIGHBOR_BEFORE = 1;
    private static final int CONTEXT_NEIGHBOR_AFTER = 2;

    private final RagUnitQueryRepository ragUnitQueryRepository;
    private final VectorStoreWriteService vectorStoreWriteService;

    /**
     * 扩展命中的叶子节点上下文（前后各 N 个邻居），并格式化为知识文本。
     */
    public String buildExpandedKnowledgeText(List<Document> rankedDocs, List<RagUnit> matchedLeafUnits) {
        Map<String, RagUnit> matchedLeafMap = RagUnitQueryRepository.toUnitMap(matchedLeafUnits);
        Map<String, RagUnit> expandedUnits = new LinkedHashMap<>();

        for (Document doc : rankedDocs) {
            RagUnit matchedLeaf = matchedLeafMap.get(doc.getId());
            if (matchedLeaf == null) {
                continue;
            }
            List<RagUnit> contextLeaves = ragUnitQueryRepository.selectNeighborLeaves(
                    matchedLeaf, CONTEXT_NEIGHBOR_BEFORE, CONTEXT_NEIGHBOR_AFTER);
            if (contextLeaves.isEmpty()) {
                expandedUnits.putIfAbsent(matchedLeaf.getId(), matchedLeaf);
                continue;
            }
            for (RagUnit contextLeaf : contextLeaves) {
                if (contextLeaf.getId() != null) {
                    expandedUnits.putIfAbsent(contextLeaf.getId(), contextLeaf);
                }
            }
        }

        if (expandedUnits.isEmpty()) {
            return rankedDocs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n---\n\n"));
        }

        Map<String, RagUnit> parentSections = ragUnitQueryRepository.selectParentSections(expandedUnits.values());
        return expandedUnits.values().stream()
                .map(unit -> formatKnowledgeUnit(unit, findParentSection(unit, parentSections)))
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * 将 RagUnit 列表转为 Document 列表（供 rerank 使用）。
     */
    public List<Document> buildDocuments(List<RagUnit> units) {
        List<Document> documents = new ArrayList<>();
        for (RagUnit unit : units) {
            if (unit.getContent() == null || unit.getContent().isBlank()) {
                continue;
            }
            documents.add(new Document(unit.getId(), unit.getContent(),
                    vectorStoreWriteService.buildVectorMetadata(unit, unit.getFilename())));
        }
        return documents;
    }

    /**
     * 从 Document 列表提取 ID。
     */
    public List<String> extractIds(List<Document> documents) {
        List<String> ids = new ArrayList<>();
        for (Document document : documents) {
            if (document.getId() != null) {
                ids.add(document.getId());
            }
        }
        return ids;
    }

    /**
     * 批量按 ID 查询 RagUnit 并转为 Map。
     */
    public Map<String, RagUnit> selectUnitsAsMap(List<String> ids) {
        return RagUnitQueryRepository.toUnitMap(ragUnitQueryRepository.selectByIds(ids));
    }

    private RagUnit findParentSection(RagUnit unit, Map<String, RagUnit> parentSections) {
        if (unit.getParentId() == null || unit.getParentId().isBlank()) {
            return null;
        }
        return parentSections.get(unit.getParentId());
    }

    private String formatKnowledgeUnit(RagUnit unit, RagUnit parentSection) {
        StringBuilder builder = new StringBuilder();
        if (unit.getFilename() != null && !unit.getFilename().isBlank()) {
            builder.append("【文档】").append(unit.getFilename()).append('\n');
        }
        if (parentSection != null && parentSection.getTitle() != null && !parentSection.getTitle().isBlank()) {
            builder.append("【章节】").append(parentSection.getTitle()).append('\n');
        }
        if (unit.getTitle() != null && !unit.getTitle().isBlank()) {
            builder.append("【标题】").append(unit.getTitle()).append('\n');
        }
        if (unit.getChunkIndex() != null) {
            builder.append("【分段】").append(unit.getChunkIndex() + 1).append('\n');
        }
        if (unit.getContent() != null) {
            builder.append(unit.getContent());
        }
        return builder.toString().trim();
    }
}
