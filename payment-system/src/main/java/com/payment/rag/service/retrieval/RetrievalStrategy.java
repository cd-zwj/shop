package com.payment.rag.service.retrieval;

import com.payment.rag.model.dto.RetrievalResult;

/**
 * 检索策略接口。
 * <p>
 * 实现类负责一种完整的检索路径（层次检索、平铺检索等），
 * 返回 null 表示该策略未命中，由编排层决定是否回退到其他策略。
 */
public interface RetrievalStrategy {

    /**
     * 执行检索。
     *
     * @param query       用户查询
     * @param userId      用户 ID（用于过滤）
     * @param topK        最终返回的最大文档数
     * @param hitThreshold 命中阈值
     * @param startTime   检索开始时间（用于计算耗时）
     * @return 检索结果，null 表示该策略未命中
     */
    RetrievalResult retrieve(String query, String userId, int topK, double hitThreshold, long startTime);
}
