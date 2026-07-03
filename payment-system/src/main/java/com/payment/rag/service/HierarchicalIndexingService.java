package com.payment.rag.service;

import com.payment.rag.Config.HierarchyConfig;
import com.payment.rag.model.RagNodeType;
import com.payment.rag.model.RagUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class HierarchicalIndexingService {

    private final HierarchyConfig hierarchyConfig;
    private final HierarchySummaryService hierarchySummaryService;

    public HierarchicalIndexingService(HierarchyConfig hierarchyConfig,
                                       HierarchySummaryService hierarchySummaryService) {
        this.hierarchyConfig = hierarchyConfig;
        this.hierarchySummaryService = hierarchySummaryService;
    }

    public List<RagUnit> buildHierarchy(String sourceId, String filename, List<RagUnit> leafUnits) {
        // 入口先统一规范化叶子节点；如果层次化关闭，就退化为普通平铺切片。
        List<RagUnit> normalizedLeaves = normalizeLeaves(sourceId, leafUnits);
        if (!hierarchyConfig.isEnabled() || normalizedLeaves.isEmpty()) {
            return normalizedLeaves;
        }

        // 三层结构固定为：leaf -> section summary -> document summary。
        List<RagUnit> sectionNodes = buildSectionNodes(sourceId, filename, normalizedLeaves);
        RagUnit docNode = buildDocumentNode(sourceId, filename, sectionNodes);

        // section 汇总节点统一挂到文档根节点下面，便于后续按 parentId 向上/向下回溯。
        for (RagUnit sectionNode : sectionNodes) {
            sectionNode.setParentId(docNode.getId());
        }

        List<RagUnit> allNodes = new ArrayList<>(normalizedLeaves.size() + sectionNodes.size() + 1);
        allNodes.addAll(normalizedLeaves);
        allNodes.addAll(sectionNodes);
        allNodes.add(docNode);
        return allNodes;
    }

    public int countLeafNodes(List<RagUnit> units) {
        int count = 0;
        for (RagUnit unit : units) {
            if (unit.getNodeType() == RagNodeType.LEAF) {
                count++;
            }
        }
        return count;
    }

    private List<RagUnit> normalizeLeaves(String sourceId, List<RagUnit> leafUnits) {
        List<RagUnit> normalizedLeaves = new ArrayList<>();
        if (leafUnits == null) {
            return normalizedLeaves;
        }

        for (int i = 0; i < leafUnits.size(); i++) {
            RagUnit leaf = leafUnits.get(i);
            // processor 只负责切内容；树结构相关字段统一在这里补齐。
            leaf.setId(UUID.randomUUID().toString());
            leaf.setSourceId(sourceId);
            leaf.setNodeType(RagNodeType.LEAF);
            leaf.setTreeLevel(0);
            leaf.setOrdinal(i);
            leaf.setChildCount(0);
            leaf.setParentId(null);
            if (leaf.getChunkIndex() == null) {
                leaf.setChunkIndex(i);
            }
            normalizedLeaves.add(leaf);
        }
        return normalizedLeaves;
    }

    private List<RagUnit> buildSectionNodes(String sourceId, String filename, List<RagUnit> leaves) {
        List<RagUnit> sectionNodes = new ArrayList<>();
        // 当前按固定窗口聚合叶子节点，先把稳定性做出来，后面再升级为更细的语义分组。
        int groupSize = Math.max(1, hierarchyConfig.getMidChildCount());

        for (int start = 0, sectionIndex = 0; start < leaves.size(); start += groupSize, sectionIndex++) {
            int end = Math.min(start + groupSize, leaves.size());
            List<RagUnit> group = leaves.subList(start, end);

            HierarchySummaryService.SummaryPayload payload =
                    hierarchySummaryService.summarizeSection(filename, sectionIndex, group);

            RagUnit sectionNode = new RagUnit();
            sectionNode.setId(UUID.randomUUID().toString());
            sectionNode.setSourceId(sourceId);
            sectionNode.setSourceType(group.get(0).getSourceType());
            sectionNode.setNodeType(RagNodeType.SECTION_SUMMARY);
            sectionNode.setTreeLevel(1);
            sectionNode.setOrdinal(sectionIndex);
            sectionNode.setTitle(payload.getTitle());
            sectionNode.setContent(payload.getSummary());
            sectionNode.setChildCount(group.size());
            sectionNode.setChunkIndex(null);

            // 叶子节点回填父节点，检索命中 section 时可以直接展开到叶子层。
            for (RagUnit leaf : group) {
                leaf.setParentId(sectionNode.getId());
            }

            sectionNodes.add(sectionNode);
        }

        return sectionNodes;
    }

    private RagUnit buildDocumentNode(String sourceId, String filename, List<RagUnit> sectionNodes) {
        // 根摘要只消费 section summary，避免再次把整篇原文直接送给模型。
        HierarchySummaryService.SummaryPayload payload =
                hierarchySummaryService.summarizeDocument(filename, sectionNodes);

        RagUnit docNode = new RagUnit();
        docNode.setId(UUID.randomUUID().toString());
        docNode.setSourceId(sourceId);
        docNode.setSourceType(sectionNodes.get(0).getSourceType());
        docNode.setNodeType(RagNodeType.DOC_SUMMARY);
        docNode.setTreeLevel(2);
        docNode.setOrdinal(0);
        docNode.setTitle(payload.getTitle());
        docNode.setContent(payload.getSummary());
        docNode.setChildCount(sectionNodes.size());
        docNode.setChunkIndex(null);
        docNode.setParentId(null);
        return docNode;
    }
}
