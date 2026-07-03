package com.payment.rag.service.retrieval;

import com.payment.rag.Config.HierarchyConfig;
import com.payment.rag.model.RagNodeType;
import com.payment.rag.model.RagUnit;
import com.payment.rag.model.dto.HierarchyHit;
import com.payment.rag.model.dto.RetrievalMode;
import com.payment.rag.model.dto.RetrievalResult;
import com.payment.rag.repository.RagUnitQueryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 层次检索策略：摘要召回 → Section 展开 → 叶子下钻 → Rerank。
 * <p>
 * 任一阶段拿不到有效结果时返回 null，由编排层回退到平铺检索。
 */
@Slf4j
@Component
public class HierarchicalRetrievalStrategy implements RetrievalStrategy {

    private final VectorStore summaryVectorStore;
    private final RagUnitQueryRepository ragUnitQueryRepository;
    private final RerankHelper rerankHelper;
    private final KnowledgeTextBuilder knowledgeTextBuilder;
    private final HierarchyConfig hierarchyConfig;
    private final UserFilterBuilder userFilterBuilder;

    @Value("${rag.retrieval.similarity-threshold:0.3}")
    private double similarityThreshold;

    public HierarchicalRetrievalStrategy(
            @Qualifier("summaryVectorStore") VectorStore summaryVectorStore,
            RagUnitQueryRepository ragUnitQueryRepository,
            RerankHelper rerankHelper,
            KnowledgeTextBuilder knowledgeTextBuilder,
            HierarchyConfig hierarchyConfig,
            UserFilterBuilder userFilterBuilder) {
        this.summaryVectorStore = summaryVectorStore;
        this.ragUnitQueryRepository = ragUnitQueryRepository;
        this.rerankHelper = rerankHelper;
        this.knowledgeTextBuilder = knowledgeTextBuilder;
        this.hierarchyConfig = hierarchyConfig;
        this.userFilterBuilder = userFilterBuilder;
    }

    @Override
    public RetrievalResult retrieve(String query, String userId, int topK, double hitThreshold, long startTime) {
        // 第一步：从摘要向量库召回候选节点
        SearchRequest summarySearch = SearchRequest.builder()
                .query(query)
                .topK(hierarchyConfig.getSummaryCandidateTopK())
                .similarityThreshold(similarityThreshold)
                .filterExpression(userFilterBuilder.build(userId))
                .build();

        List<Document> summaryCandidates = summaryVectorStore.similaritySearch(summarySearch);
        if (summaryCandidates.isEmpty()) {
            log.info("层次检索未召回摘要节点，回退到叶子平铺检索");
            return null;
        }

        List<RagUnit> summaryUnits = ragUnitQueryRepository.selectByIds(knowledgeTextBuilder.extractIds(summaryCandidates));
        if (summaryUnits.isEmpty()) {
            log.info("层次检索摘要候选无法映射到节点，回退到叶子平铺检索");
            return null;
        }

        // 第二步：展开 section 节点
        Map<String, RagUnit> summaryUnitMap = RagUnitQueryRepository.toUnitMap(summaryUnits);
        List<RagUnit> expandedSections = expandSectionCandidates(summaryCandidates, summaryUnitMap);
        if (expandedSections.isEmpty()) {
            log.info("层次检索无法展开中层节点，回退到叶子平铺检索");
            return null;
        }

        List<Document> sectionDocs = knowledgeTextBuilder.buildDocuments(expandedSections);
        RerankHelper.ScoredDocumentsResult sectionRerank = rerankHelper.rerank(query, sectionDocs, hierarchyConfig.getMidRerankTopK());
        if (sectionRerank.documents().isEmpty()) {
            log.info("层次检索中层 rerank 为空，回退到叶子平铺检索");
            return null;
        }

        // 第三步：下钻到叶子节点
        List<String> sectionIds = knowledgeTextBuilder.extractIds(sectionRerank.documents());
        List<RagUnit> leafUnits = ragUnitQueryRepository.selectChildrenByParentIds(userId, sectionIds, RagNodeType.LEAF);
        if (leafUnits.isEmpty()) {
            log.info("层次检索中层下钻后没有叶子节点，回退到叶子平铺检索");
            return null;
        }

        List<Document> leafDocs = knowledgeTextBuilder.buildDocuments(leafUnits);
        RerankHelper.ScoredDocumentsResult leafRerank = rerankHelper.rerank(
                query, leafDocs, Math.min(topK, hierarchyConfig.getLeafRerankTopK()));
        if (leafRerank.documents().isEmpty()) {
            log.info("层次检索叶子 rerank 为空，回退到叶子平铺检索");
            return null;
        }

        if (leafRerank.topScore() == null || leafRerank.topScore() < hitThreshold) {
            log.info("层次检索叶子最高分低于阈值，回退到叶子平铺检索: score={}", leafRerank.topScore());
            return null;
        }

        // 构建层级命中路径
        Map<String, RagUnit> leafUnitMap = knowledgeTextBuilder.selectUnitsAsMap(knowledgeTextBuilder.extractIds(leafRerank.documents()));
        Map<String, RagUnit> sectionUnitMap = new HashMap<>(RagUnitQueryRepository.toUnitMap(expandedSections));

        Set<String> docIds = new LinkedHashSet<>();
        for (RagUnit section : expandedSections) {
            if (section.getParentId() != null) {
                docIds.add(section.getParentId());
            }
        }
        Map<String, RagUnit> docUnitMap = knowledgeTextBuilder.selectUnitsAsMap(new ArrayList<>(docIds));

        List<HierarchyHit> hierarchyHits = buildHierarchyHits(
                leafRerank.documents(), leafUnitMap, sectionUnitMap, docUnitMap,
                sectionRerank.scoreById(), leafRerank.scoreById());

        long duration = System.currentTimeMillis() - startTime;
        String knowledgeText = knowledgeTextBuilder.buildExpandedKnowledgeText(leafRerank.documents(), ragUnitQueryRepository.selectByIds(knowledgeTextBuilder.extractIds(leafRerank.documents())));

        log.info("层次检索完成: summaryCandidates={}, expandedSections={}, finalLeaves={}, topScore={}, 耗时={}ms",
                summaryCandidates.size(), expandedSections.size(), leafRerank.documents().size(),
                leafRerank.topScore() != null ? String.format("%.4f", leafRerank.topScore()) : "N/A", duration);

        return RetrievalResult.builder()
                .documents(leafRerank.documents())
                .hit(true)
                .retrievalMode(RetrievalMode.HIERARCHICAL)
                .knowledgeText(knowledgeText)
                .candidateCount(summaryCandidates.size())
                .finalCount(leafRerank.documents().size())
                .durationMs(duration)
                .hierarchyHits(hierarchyHits)
                .build();
    }

    private List<RagUnit> expandSectionCandidates(List<Document> summaryCandidates, Map<String, RagUnit> summaryUnitMap) {
        Set<String> sectionIds = new LinkedHashSet<>();
        Set<String> docIds = new LinkedHashSet<>();

        for (Document candidate : summaryCandidates) {
            RagUnit unit = summaryUnitMap.get(candidate.getId());
            if (unit == null || unit.getNodeType() == null) {
                continue;
            }
            if (unit.getNodeType() == RagNodeType.SECTION_SUMMARY) {
                sectionIds.add(unit.getId());
            } else if (unit.getNodeType() == RagNodeType.DOC_SUMMARY) {
                docIds.add(unit.getId());
            }
        }

        List<RagUnit> sections = new ArrayList<>();
        if (!sectionIds.isEmpty()) {
            sections.addAll(ragUnitQueryRepository.selectByIds(new ArrayList<>(sectionIds)));
        }
        if (!docIds.isEmpty()) {
            sections.addAll(ragUnitQueryRepository.selectChildrenByParentIds(null, new ArrayList<>(docIds), RagNodeType.SECTION_SUMMARY));
        }

        Map<String, RagUnit> deduped = new java.util.LinkedHashMap<>();
        for (RagUnit section : sections) {
            deduped.put(section.getId(), section);
        }
        return new ArrayList<>(deduped.values());
    }

    private List<HierarchyHit> buildHierarchyHits(List<Document> finalDocs,
                                                  Map<String, RagUnit> leafUnitMap,
                                                  Map<String, RagUnit> sectionUnitMap,
                                                  Map<String, RagUnit> docUnitMap,
                                                  Map<String, Double> sectionScores,
                                                  Map<String, Double> leafScores) {
        List<HierarchyHit> hits = new ArrayList<>();
        for (Document doc : finalDocs) {
            RagUnit leafUnit = leafUnitMap.get(doc.getId());
            if (leafUnit == null) {
                continue;
            }

            RagUnit sectionUnit = leafUnit.getParentId() != null
                    ? sectionUnitMap.get(leafUnit.getParentId())
                    : null;
            RagUnit docUnit = sectionUnit != null && sectionUnit.getParentId() != null
                    ? docUnitMap.get(sectionUnit.getParentId())
                    : null;

            hits.add(HierarchyHit.builder()
                    .sourceId(leafUnit.getSourceId())
                    .docNodeId(docUnit != null ? docUnit.getId() : null)
                    .docTitle(docUnit != null ? docUnit.getTitle() : null)
                    .sectionNodeId(sectionUnit != null ? sectionUnit.getId() : null)
                    .sectionTitle(sectionUnit != null ? sectionUnit.getTitle() : null)
                    .leafUnitId(leafUnit.getId())
                    .leafChunkIndex(leafUnit.getChunkIndex())
                    .summaryScore(sectionUnit != null ? sectionScores.get(sectionUnit.getId()) : null)
                    .leafScore(leafScores.get(leafUnit.getId()))
                    .content(leafUnit.getContent())
                    .filename(leafUnit.getFilename())
                    .minioUrl(leafUnit.getMinioUrl())
                    .build());
        }
        return hits;
    }
}
