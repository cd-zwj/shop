package com.payment.rag.service.retrieval;

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
import java.util.List;
import java.util.Map;

/**
 * 平铺检索策略：直接从叶子向量库召回并 rerank。
 * <p>
 * 既可作为独立检索路径，也可作为层次检索失败时的兜底。
 */
@Slf4j
@Component
public class FlatRetrievalStrategy implements RetrievalStrategy {

    private final VectorStore leafVectorStore;
    private final RagUnitQueryRepository ragUnitQueryRepository;
    private final RerankHelper rerankHelper;
    private final KnowledgeTextBuilder knowledgeTextBuilder;
    private final UserFilterBuilder userFilterBuilder;

    @Value("${rag.retrieval.candidate-top-k:15}")
    private int candidateTopK;

    @Value("${rag.retrieval.similarity-threshold:0.3}")
    private double similarityThreshold;

    public FlatRetrievalStrategy(
            @Qualifier("leafVectorStore") VectorStore leafVectorStore,
            RagUnitQueryRepository ragUnitQueryRepository,
            RerankHelper rerankHelper,
            KnowledgeTextBuilder knowledgeTextBuilder,
            UserFilterBuilder userFilterBuilder) {
        this.leafVectorStore = leafVectorStore;
        this.ragUnitQueryRepository = ragUnitQueryRepository;
        this.rerankHelper = rerankHelper;
        this.knowledgeTextBuilder = knowledgeTextBuilder;
        this.userFilterBuilder = userFilterBuilder;
    }

    @Override
    public RetrievalResult retrieve(String query, String userId, int topK, double hitThreshold, long startTime) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(candidateTopK)
                .similarityThreshold(similarityThreshold)
                .filterExpression(userFilterBuilder.build(userId))
                .build();

        List<Document> candidates = leafVectorStore.similaritySearch(searchRequest);
        if (candidates.isEmpty()) {
            long duration = System.currentTimeMillis() - startTime;
            log.info("未召回任何叶子候选文档, 耗时={}ms", duration);
            return RetrievalResult.empty(duration);
        }

        RerankHelper.ScoredDocumentsResult rerankResult = rerankHelper.rerank(query, candidates, topK);
        boolean hit = rerankResult.topScore() != null && rerankResult.topScore() >= hitThreshold;
        long duration = System.currentTimeMillis() - startTime;
        log.info("平铺检索完成: hit={}, candidates={}, final={}, topScore={}, 耗时={}ms",
                hit, candidates.size(), rerankResult.documents().size(),
                rerankResult.topScore() != null ? String.format("%.4f", rerankResult.topScore()) : "N/A", duration);

        List<String> leafIds = knowledgeTextBuilder.extractIds(rerankResult.documents());
        List<RagUnit> leafUnits = ragUnitQueryRepository.selectByIds(leafIds);
        String knowledgeText = knowledgeTextBuilder.buildExpandedKnowledgeText(rerankResult.documents(), leafUnits);
        Map<String, RagUnit> leafUnitMap = RagUnitQueryRepository.toUnitMap(leafUnits);

        List<HierarchyHit> hierarchyHits = new ArrayList<>();
        for (Document doc : rerankResult.documents()) {
            RagUnit leafUnit = leafUnitMap.get(doc.getId());
            if (leafUnit == null) continue;
            hierarchyHits.add(HierarchyHit.builder()
                    .sourceId(leafUnit.getSourceId())
                    .leafUnitId(leafUnit.getId())
                    .leafChunkIndex(leafUnit.getChunkIndex())
                    .leafScore(rerankResult.scoreById().get(leafUnit.getId()))
                    .content(leafUnit.getContent())
                    .filename(leafUnit.getFilename())
                    .minioUrl(leafUnit.getMinioUrl())
                    .build());
        }

        return RetrievalResult.builder()
                .documents(rerankResult.documents())
                .hit(hit)
                .retrievalMode(RetrievalMode.FLAT_FALLBACK)
                .knowledgeText(knowledgeText)
                .candidateCount(candidates.size())
                .finalCount(rerankResult.documents().size())
                .durationMs(duration)
                .hierarchyHits(hierarchyHits)
                .build();
    }
}
