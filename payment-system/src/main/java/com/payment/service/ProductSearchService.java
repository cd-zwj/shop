package com.payment.service;

import com.payment.entity.Product;

import java.util.List;

/**
 * 商品搜索服务接口
 */
public interface ProductSearchService {
    
    /**
     * 同步商品到Elasticsearch
     */
    void syncProduct(Product product);
    
    /**
     * 从Elasticsearch删除商品
     */
    void deleteProduct(Long productId);
    
    /**
     * 搜索商品
     */
    List<Product> searchProducts(String keyword, Long tenantId);
    
    /**
     * 按分类搜索商品
     */
    List<Product> searchByCategory(String category);
}
