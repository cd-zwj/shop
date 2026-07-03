package com.payment.rag.service.retrieval;

import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Rerank 工具类，封装 rerank 模型调用和熔断降级逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RerankHelper {

    private final RerankModel rerankModel;

    @CircuitBreaker(name = "dashscope-rerank", fallbackMethod = "rerankFallback")
    public ScoredDocumentsResult rerank(String query, List<Document> candidates, int topK) {
        RerankRequest request = new RerankRequest(query, candidates);
        RerankResponse response = rerankModel.call(request);

        List<DocumentWithScore> scoredDocs = response.getResults();
        List<Document> result = new ArrayList<>();
        Map<String, Double> scoreById = new HashMap<>();
        Double topScore = null;

        for (int i = 0; i < Math.min(topK, scoredDocs.size()); i++) {
            DocumentWithScore dws = scoredDocs.get(i);
            Document output = dws.getOutput();
            result.add(output);
            scoreById.put(output.getId(), dws.getScore());
            if (i == 0) {
                topScore = dws.getScore();
            }
        }

        return new ScoredDocumentsResult(result, scoreById, topScore);
    }

    public ScoredDocumentsResult rerankFallback(String query, List<Document> candidates, int topK, Throwable t) {
        log.warn("rerank 熔断降级，回退为原始排序: error={}", t.getMessage());
        List<Document> fallback = candidates.stream()
                .limit(topK)
                .collect(Collectors.toList());
        Map<String, Double> scoreById = new HashMap<>();
        for (Document doc : fallback) {
            scoreById.put(doc.getId(), doc.getScore());
        }
        Double fallbackScore = (!fallback.isEmpty() && fallback.get(0).getScore() != null)
                ? fallback.get(0).getScore() : null;
        return new ScoredDocumentsResult(fallback, scoreById, fallbackScore);
    }

    public record ScoredDocumentsResult(List<Document> documents, Map<String, Double> scoreById, Double topScore) {
    }
}
