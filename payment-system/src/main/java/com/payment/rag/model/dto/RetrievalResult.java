package com.payment.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.document.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResult {

    private List<Document> documents;
    private boolean hit;
    private RetrievalMode retrievalMode;
    private String knowledgeText;
    private int candidateCount;
    private int finalCount;
    private long durationMs;
    private List<HierarchyHit> hierarchyHits;

    public static RetrievalResult empty(long durationMs) {
        return RetrievalResult.builder()
                .documents(List.of())
                .hit(false)
                .retrievalMode(RetrievalMode.FLAT_FALLBACK)
                .knowledgeText("")
                .candidateCount(0)
                .finalCount(0)
                .durationMs(durationMs)
                .hierarchyHits(List.of())
                .build();
    }
}
