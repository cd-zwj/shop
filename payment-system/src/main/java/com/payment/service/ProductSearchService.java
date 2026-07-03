package com.payment.service;

import com.payment.entity.Product;

import java.util.List;

/**
 * 商品搜索服务接口。
 *
 * <p>基于 Elasticsearch 提供商品全文搜索能力，支持关键字搜索和分类检索。
 * 同时负责商品数据的索引同步（增删改时维护ES索引一致性）。</p>
 */
public interface ProductSearchService {

    /**
     * 同步单个商品到 Elasticsearch 索引。
     *
     * <p>商品创建或更新后调用，确保ES索引与数据库数据一致。</p>
     *
     * @param product 待同步的商品实体
     */
    void syncProduct(Product product);

    /**
     * 从 Elasticsearch 索引中删除指定商品。
     *
     * <p>商品逻辑删除或物理删除后调用。</p>
     *
     * @param productId 商品ID
     */
    void deleteProduct(Long productId);

    /**
     * 按关键字搜索商品。
     *
     * @param keyword  搜索关键字（匹配商品名称、描述等字段）
     * @param tenantId 租户ID，用于限定搜索范围
     * @return 匹配的商品列表
     */
    List<Product> searchProducts(String keyword, Long tenantId);

    /**
     * 按分类搜索商品。
     *
     * @param category 商品分类名称
     * @return 该分类下的商品列表
     */
    List<Product> searchByCategory(String category);
}
