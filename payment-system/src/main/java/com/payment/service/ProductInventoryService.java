package com.payment.service;

/**
 * 商品库存服务。
 */
public interface ProductInventoryService {

    void deductStock(Long tenantId, Long productId, Integer quantity, String bizNo);
}
