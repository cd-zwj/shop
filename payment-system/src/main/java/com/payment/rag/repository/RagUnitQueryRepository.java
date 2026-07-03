package com.payment.rag.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.payment.rag.mapper.RagUnitMapper;
import com.payment.rag.model.RagNodeType;
import com.payment.rag.model.RagUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RagUnit 查询仓库，封装 MyBatis 直接查询逻辑。
 * <p>
 * 从 RagRetrievalService 中抽取而来，使 Retrieval 层专注于检索编排而非数据访问细节。
 */
@Repository
@RequiredArgsConstructor
public class RagUnitQueryRepository {

    private final RagUnitMapper ragUnitMapper;

    /**
     * 批量按 ID 查询 RagUnit。
     */
    public List<RagUnit> selectByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ragUnitMapper.selectBatchIds(ids);
    }

    /**
     * 查询指定叶子节点的邻居叶子（用于上下文窗口扩展）。
     */
    public List<RagUnit> selectNeighborLeaves(RagUnit leaf, int before, int after) {
        if (leaf.getSourceId() == null || leaf.getChunkIndex() == null) {
            return List.of(leaf);
        }

        int start = Math.max(0, leaf.getChunkIndex() - before);
        int end = leaf.getChunkIndex() + after;
        QueryWrapper<RagUnit> wrapper = new QueryWrapper<>();
        wrapper.eq("source_id", leaf.getSourceId())
                .eq(leaf.getUserId() != null && !leaf.getUserId().isBlank(), "user_id", leaf.getUserId())
                .and(group -> group
                        .eq("node_type", RagNodeType.LEAF.name())
                        .or()
                        .isNull("node_type"))
                .between("chunk_index", start, end)
                .orderByAsc("chunk_index");

        List<RagUnit> neighbors = ragUnitMapper.selectList(wrapper);
        return (neighbors == null || neighbors.isEmpty()) ? List.of(leaf) : neighbors;
    }

    /**
     * 按父节点 ID 查询子节点。
     */
    public List<RagUnit> selectChildrenByParentIds(String userId, Collection<String> parentIds, RagNodeType nodeType) {
        if (parentIds == null || parentIds.isEmpty()) {
            return List.of();
        }
        QueryWrapper<RagUnit> wrapper = new QueryWrapper<>();
        wrapper.in("parent_id", parentIds)
                .eq("node_type", nodeType.name())
                .eq(userId != null && !userId.isBlank(), "user_id", userId)
                .orderByAsc("ordinal")
                .orderByAsc("chunk_index");
        return ragUnitMapper.selectList(wrapper);
    }

    /**
     * 关键词搜索叶子节点（标题/内容/文件名模糊匹配）。
     */
    public List<RagUnit> searchLeafUnitsByKeyword(String keyword, String userId, int limit) {
        QueryWrapper<RagUnit> wrapper = new QueryWrapper<>();
        wrapper.and(group -> group
                        .eq("node_type", RagNodeType.LEAF.name())
                        .or()
                        .isNull("node_type"))
                .eq(userId != null && !userId.isBlank(), "user_id", userId)
                .and(group -> group
                        .like("title", keyword)
                        .or()
                        .like("content", keyword)
                        .or()
                        .like("filename", keyword))
                .orderByAsc("chunk_index")
                .last("LIMIT " + Math.max(limit, 1));
        return ragUnitMapper.selectList(wrapper);
    }

    /**
     * 查询父节点集合（按 parentId 去重后批量查）。
     */
    public Map<String, RagUnit> selectParentSections(Collection<RagUnit> units) {
        Set<String> parentIds = new LinkedHashSet<>();
        for (RagUnit unit : units) {
            if (unit.getParentId() != null && !unit.getParentId().isBlank()) {
                parentIds.add(unit.getParentId());
            }
        }
        if (parentIds.isEmpty()) {
            return Map.of();
        }
        return toUnitMap(selectByIds(new ArrayList<>(parentIds)));
    }

    /**
     * 将 RagUnit 列表转为 id → unit 的 Map。
     */
    public static Map<String, RagUnit> toUnitMap(List<RagUnit> units) {
        if (units == null || units.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, RagUnit> map = new HashMap<>();
        for (RagUnit unit : units) {
            map.put(unit.getId(), unit);
        }
        return map;
    }
}
