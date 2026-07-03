package com.payment.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HierarchyHit {
    private String sourceId;
    private String docNodeId;
    private String docTitle;
    private String sectionNodeId;
    private String sectionTitle;
    private String leafUnitId;
    private Integer leafChunkIndex;
    private Double summaryScore;
    private Double leafScore;
    private String content;
    private String filename;
    private String minioUrl;
}
