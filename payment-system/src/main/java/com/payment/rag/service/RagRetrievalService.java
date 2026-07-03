package com.payment.rag.service;

import com.payment.rag.model.dto.RetrievalResult;
import com.payment.rag.repository.RagUnitQueryRepository;
import com.payment.rag.service.retrieval.FlatRetrievalStrategy;
import com.payment.rag.service.retrieval.HierarchicalRetrievalStrategy;
import com.payment.rag.service.retrieval.KnowledgeTextBuilder;
import com.payment.rag.service.retrieval.RerankHelper;
import com.payment.rag.service.retrieval.UserFilterBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

/**
 * 检索编排服务，负责策略选择、多路召回和 fallback 控制。
 * <p>
 * 具体的检索路径已拆分为独立策略：
 * <ul>
 *   <li>{@link HierarchicalRetrievalStrategy} — 摘要→Section→叶子层次检索</li>
 *   <li>{@link FlatRetrievalStrategy} — 叶子向量库平铺检索</li>
 * </ul>
 */
@Service
@Slf4j
public class RagRetrievalService {

    private final VectorStore leafVectorStore;
    private final HierarchicalRetrievalStrategy hierarchicalStrategy;
    private final FlatRetrievalStrategy flatStrategy;
    private final RerankHelper rerankHelper;
    private final KnowledgeTextBuilder knowledgeTextBuilder;
    private final RagUnitQueryRepository ragUnitQueryRepository;
    private final RagUnitService ragUnitService;
    private final VectorStoreWriteService vectorStoreWriteService;
    private final UserFilterBuilder userFilterBuilder;
    private final Executor retrievalTaskExecutor;

    @Value("${rag.retrieval.candidate-top-k:15}")
    private int candidateTopK;

    @Value("${rag.retrieval.similarity-threshold:0.3}")
    private double similarityThreshold;

    @Value("${rag.retrieval.final-top-k:5}")
    private int finalTopK;

    @Value("${rag.retrieval.hit-score-threshold:0.35}")
    private double hitScoreThreshold;

    public RagRetrievalService(@Qualifier("leafVectorStore") VectorStore leafVectorStore,
                               HierarchicalRetrievalStrategy hierarchicalStrategy,
                               FlatRetrievalStrategy flatStrategy,
                               RerankHelper rerankHelper,
                               KnowledgeTextBuilder knowledgeTextBuilder,
                               RagUnitQueryRepository ragUnitQueryRepository,
                               RagUnitService ragUnitService,
                               VectorStoreWriteService vectorStoreWriteService,
                               UserFilterBuilder userFilterBuilder,
                               @Qualifier("mvcTaskExecutor") Executor retrievalTaskExecutor) {
        this.leafVectorStore = leafVectorStore;
        this.hierarchicalStrategy = hierarchicalStrategy;
        this.flatStrategy = flatStrategy;
        this.rerankHelper = rerankHelper;
        this.knowledgeTextBuilder = knowledgeTextBuilder;
        this.ragUnitQueryRepository = ragUnitQueryRepository;
        this.ragUnitService = ragUnitService;
        this.vectorStoreWriteService = vectorStoreWriteService;
        this.userFilterBuilder = userFilterBuilder;
        this.retrievalTaskExecutor = retrievalTaskExecutor;
    }

    // ────── 公开 API ──────

    public RetrievalResult retrieve(String query) {
        return retrieve(query, null, finalTopK, hitScoreThreshold);
    }

    public RetrievalResult retrieve(String query, String userId) {
        return retrieve(query, userId, finalTopK, hitScoreThreshold);
    }

    public RetrievalResult retrieve(String query, int topK, double hitThreshold) {
        return retrieve(query, null, topK, hitScoreThreshold);
    }

    public RetrievalResult retrieve(String query, String userId, int topK, double hitThreshold) {
        long startTime = System.currentTimeMillis();
        try {
            // 优先层次检索；失败则回退平铺检索
            RetrievalResult hierarchical = hierarchicalStrategy.retrieve(query, userId, topK, hitThreshold, startTime);
            if (hierarchical != null) {
                return hierarchical;
            }
            return flatStrategy.retrieve(query, userId, topK, hitThreshold, startTime);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("检索过程发生异常, 耗时={}ms", duration, e);
            return RetrievalResult.empty(duration);
        }
    }

    public RetrievalResult retrieveWithMultiPathRecall(String primaryQuery, List<String> recallQueries, String userId) {
        return retrieveWithMultiPathRecall(primaryQuery, recallQueries, userId, finalTopK, hitScoreThreshold);
    }

    public RetrievalResult retrieveWithMultiPathRecall(String primaryQuery,
                                                       List<String> recallQueries,
                                                       String userId,
                                                       int topK,
                                                       double hitThreshold) {
        long startTime = System.currentTimeMillis();
        List<String> normalizedQueries = normalizeQueries(primaryQuery, recallQueries);

        try {
            if (normalizedQueries.isEmpty()) {
                return RetrievalResult.empty(System.currentTimeMillis() - startTime);
            }

            RetrievalResult primaryResult = retrieve(normalizedQueries.get(0), userId, topK, hitThreshold);
            if (normalizedQueries.size() == 1) {
                if (primaryResult.isHit() || !primaryResult.getDocuments().isEmpty()) {
                    return primaryResult;
                }
                return fallbackToKeywordRecall(normalizedQueries, userId, topK, hitThreshold, startTime);
            }

            List<Document> mergedCandidates = collectHybridCandidates(normalizedQueries, userId);
            if (mergedCandidates.isEmpty()) {
                long duration = System.currentTimeMillis() - startTime;
                log.info("多路召回未召回任何候选文档, queries={}, 耗时={}ms", normalizedQueries.size(), duration);
                if (!primaryResult.getDocuments().isEmpty()) {
                    return primaryResult;
                }
                return RetrievalResult.empty(duration);
            }

            RerankHelper.ScoredDocumentsResult rerankResult = rerankHelper.rerank(normalizedQueries.get(0), mergedCandidates, topK);
            boolean hit = rerankResult.topScore() != null && rerankResult.topScore() >= hitThreshold;
            if (!hit && primaryResult.isHit()) {
                log.info("多路召回结果未达命中阈值，回退到主查询检索结果: primaryQuery={}", normalizedQueries.get(0));
                return primaryResult;
            }
            long duration = System.currentTimeMillis() - startTime;
            log.info("多路召回完成: queries={}, mergedCandidates={}, final={}, topScore={}, 耗时={}ms",
                    normalizedQueries.size(), mergedCandidates.size(), rerankResult.documents().size(),
                    rerankResult.topScore() != null ? String.format("%.4f", rerankResult.topScore()) : "N/A", duration);

            List<String> leafIds = knowledgeTextBuilder.extractIds(rerankResult.documents());
            List<com.payment.rag.model.RagUnit> leafUnits = ragUnitQueryRepository.selectByIds(leafIds);
            String knowledgeText = knowledgeTextBuilder.buildExpandedKnowledgeText(rerankResult.documents(), leafUnits);
            Map<String, com.payment.rag.model.RagUnit> leafUnitMap = RagUnitQueryRepository.toUnitMap(leafUnits);
            List<com.payment.rag.model.dto.HierarchyHit> hierarchyHits = new ArrayList<>();
            for (Document doc : rerankResult.documents()) {
                com.payment.rag.model.RagUnit leafUnit = leafUnitMap.get(doc.getId());
                if (leafUnit == null) continue;
                hierarchyHits.add(com.payment.rag.model.dto.HierarchyHit.builder()
                        .sourceId(leafUnit.getSourceId())
                        .leafUnitId(leafUnit.getId())
                        .leafChunkIndex(leafUnit.getChunkIndex())
                        .leafScore(rerankResult.scoreById().get(leafUnit.getId()))
                        .content(leafUnit.getContent())
                        .filename(leafUnit.getFilename())
                        .minioUrl(leafUnit.getMinioUrl())
                        .build());
            }

            return com.payment.rag.model.dto.RetrievalResult.builder()
                    .documents(rerankResult.documents())
                    .hit(hit)
                    .retrievalMode(com.payment.rag.model.dto.RetrievalMode.FLAT_FALLBACK)
                    .knowledgeText(knowledgeText)
                    .candidateCount(mergedCandidates.size())
                    .finalCount(rerankResult.documents().size())
                    .durationMs(duration)
                    .hierarchyHits(hierarchyHits)
                    .build();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("多路召回过程发生异常, 耗时={}ms", duration, e);
            return RetrievalResult.empty(duration);
        }
    }

    private RetrievalResult fallbackToKeywordRecall(List<String> queries,
                                                    String userId,
                                                    int topK,
                                                    double hitThreshold,
                                                    long startTime) {
        List<Document> keywordCandidates = collectKeywordCandidates(queries, userId);
        if (keywordCandidates.isEmpty()) {
            long duration = System.currentTimeMillis() - startTime;
            log.info("单路召回未召回任何候选文档，关键词回退也为空: queries={}, 耗时={}ms", queries.size(), duration);
            return RetrievalResult.empty(duration);
        }

        log.info("单路向量召回为空，已回退到关键词召回: queries={}, candidates={}", queries.size(), keywordCandidates.size());
        RerankHelper.ScoredDocumentsResult rerankResult = rerankHelper.rerank(queries.get(0), keywordCandidates, topK);
        boolean hit = rerankResult.topScore() != null && rerankResult.topScore() >= hitThreshold;
        long duration = System.currentTimeMillis() - startTime;

        List<String> leafIds = knowledgeTextBuilder.extractIds(rerankResult.documents());
        List<com.payment.rag.model.RagUnit> leafUnits = ragUnitQueryRepository.selectByIds(leafIds);
        String knowledgeText = knowledgeTextBuilder.buildExpandedKnowledgeText(rerankResult.documents(), leafUnits);
        Map<String, com.payment.rag.model.RagUnit> leafUnitMap = RagUnitQueryRepository.toUnitMap(leafUnits);
        List<com.payment.rag.model.dto.HierarchyHit> hierarchyHits = new ArrayList<>();
        for (Document doc : rerankResult.documents()) {
            com.payment.rag.model.RagUnit leafUnit = leafUnitMap.get(doc.getId());
            if (leafUnit == null) {
                continue;
            }
            hierarchyHits.add(com.payment.rag.model.dto.HierarchyHit.builder()
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
                .retrievalMode(com.payment.rag.model.dto.RetrievalMode.KEYWORD_FALLBACK)
                .knowledgeText(knowledgeText)
                .candidateCount(keywordCandidates.size())
                .finalCount(rerankResult.documents().size())
                .durationMs(duration)
                .hierarchyHits(hierarchyHits)
                .build();
    }

    public RetrievalResult retrieveWithoutRerank(String query, int topK) {
        long startTime = System.currentTimeMillis();
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build();

            List<Document> docs = leafVectorStore.similaritySearch(searchRequest);
            boolean hit = !docs.isEmpty()
                    && docs.get(0).getScore() != null
                    && docs.get(0).getScore() > 0.7;

            List<com.payment.rag.model.RagUnit> leafUnits = ragUnitQueryRepository.selectByIds(knowledgeTextBuilder.extractIds(docs));
            String knowledgeText = knowledgeTextBuilder.buildExpandedKnowledgeText(docs, leafUnits);
            long duration = System.currentTimeMillis() - startTime;

            return RetrievalResult.builder()
                    .documents(docs)
                    .hit(hit)
                    .retrievalMode(com.payment.rag.model.dto.RetrievalMode.FLAT_FALLBACK)
                    .knowledgeText(knowledgeText)
                    .candidateCount(docs.size())
                    .finalCount(docs.size())
                    .durationMs(duration)
                    .hierarchyHits(List.of())
                    .build();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("向量检索异常", e);
            return RetrievalResult.empty(duration);
        }
    }

    // ────── 多路召回内部方法 ──────

    private List<Document> collectHybridCandidates(List<String> queries, String userId) {
        List<Document> vectorCandidates = collectFlatCandidates(queries, userId);
        if (!vectorCandidates.isEmpty()) {
            return vectorCandidates;
        }

        List<Document> keywordCandidates = collectKeywordCandidates(queries, userId);
        if (!keywordCandidates.isEmpty()) {
            log.info("向量召回为空，已回退到关键词召回: queries={}, candidates={}", queries.size(), keywordCandidates.size());
        }
        return keywordCandidates;
    }

    private List<Document> collectFlatCandidates(List<String> queries, String userId) {
        List<CompletableFuture<List<Document>>> futures = queries.stream()
                .map(query -> CompletableFuture.supplyAsync(() -> searchLeafCandidates(query, userId), retrievalTaskExecutor)
                        .exceptionally(ex -> {
                            log.warn("子查询召回失败，跳过该路查询: query={}, error={}", query, ex.getMessage());
                            return List.of();
                        }))
                .toList();

        Map<String, Document> deduped = new LinkedHashMap<>();
        for (List<Document> candidates : futures.stream().map(CompletableFuture::join).toList()) {
            for (Document candidate : candidates) {
                String dedupeKey = candidate.getId() != null ? candidate.getId() : candidate.getText();
                if (dedupeKey == null || dedupeKey.isBlank()) {
                    continue;
                }
                deduped.putIfAbsent(dedupeKey, candidate);
            }
        }
        return new ArrayList<>(deduped.values());
    }

    private List<Document> searchLeafCandidates(String query, String userId) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(candidateTopK)
                .similarityThreshold(similarityThreshold)
                .filterExpression(userFilterBuilder.build(userId))
                .build();
        return leafVectorStore.similaritySearch(searchRequest);
    }

    private List<Document> collectKeywordCandidates(List<String> queries, String userId) {
        Map<String, Document> deduped = new LinkedHashMap<>();
        for (String query : queries) {
            for (String keyword : extractKeywordFallbackTerms(query)) {
                for (com.payment.rag.model.RagUnit unit : ragUnitQueryRepository.searchLeafUnitsByKeyword(keyword, userId, candidateTopK)) {
                    if (unit.getId() == null || unit.getContent() == null || unit.getContent().isBlank()) {
                        continue;
                    }
                    deduped.putIfAbsent(unit.getId(), new Document(
                            unit.getId(),
                            unit.getContent(),
                            vectorStoreWriteService.buildVectorMetadata(unit, unit.getFilename())
                    ));
                }
                if (deduped.size() >= candidateTopK) {
                    break;
                }
            }
        }
        return new ArrayList<>(deduped.values());
    }

    private List<String> normalizeQueries(String primaryQuery, List<String> recallQueries) {
        Set<String> normalized = new LinkedHashSet<>();
        if (primaryQuery != null && !primaryQuery.isBlank()) {
            normalized.add(primaryQuery.trim());
        }
        if (recallQueries != null) {
            recallQueries.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(query -> !query.isBlank())
                    .forEach(normalized::add);
        }
        return new ArrayList<>(normalized);
    }

    private String normalizeKeywordQuery(String query) {
        if (query == null) {
            return "";
        }
        String normalized = query.trim();
        normalized = normalized.replaceAll("^(请问|请介绍一下|请介绍|介绍一下|请解释一下|请解释|解释一下|请说明一下|请说明|说明一下|什么是)", "");
        normalized = normalized.replaceAll("(是什么|是啥|什么意思|含义是什么|定义是什么|的定义|的含义|吗|呢)$", "");
        normalized = normalized.replaceAll("[？?。！!，,；;：:\"\"''（）()【】\\[\\]]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    private List<String> extractKeywordFallbackTerms(String query) {
        String normalized = normalizeKeywordQuery(query);
        if (normalized.isBlank()) {
            return List.of();
        }

        Set<String> terms = new LinkedHashSet<>();
        if (normalized.length() <= 60) {
            terms.add(normalized);
        }

        String cleaned = normalized
                .replaceAll("(请优先引用|优先引用|引用|上传|文档|知识库|回答|解决方法|解决方案|有哪些|有哪|什么|如何|怎么|请|的)", " ")
                .replaceAll("\\s+", " ")
                .trim();

        Pattern separator = Pattern.compile("[\\s,，。！？?；;：:、]+");
        for (String token : separator.split(cleaned)) {
            addKeywordTerm(terms, token);
        }

        for (String token : separator.split(normalized)) {
            addKeywordTerm(terms, token);
        }

        return terms.stream()
                .filter(term -> term.length() >= 2)
                .limit(8)
                .toList();
    }

    private void addKeywordTerm(Set<String> terms, String rawToken) {
        if (rawToken == null) {
            return;
        }
        String token = rawToken.trim();
        if (token.length() < 2) {
            return;
        }
        terms.add(token);

        String stripped = token.replaceAll("(问题|方法|方案|技术|原理|机制)$", "");
        if (stripped.length() >= 2 && !stripped.equals(token)) {
            terms.add(stripped);
        }
    }
}
