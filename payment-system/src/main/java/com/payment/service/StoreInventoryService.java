package com.payment.service;

import com.payment.entity.StoreProductStock;

/**
 * 统一的门店库存账本服务。
 *
 * <p>所有线上到店自提订单必须通过该服务变更库存，避免直接修改库存表。</p>
 */
public interface StoreInventoryService {

    StoreProductStock getOrCreate(Long tenantId, Long storeId, Long productId);

    StoreProductStock adjust(Long tenantId, Long storeId, Long productId, int delta,
                             Long operatorId, String remark);

    void lock(Long tenantId, Long storeId, Long productId, int quantity, String bizType, String bizNo);

    void release(Long tenantId, Long storeId, Long productId, int quantity, String bizType, String bizNo);

    void deductLocked(Long tenantId, Long storeId, Long productId, int quantity,
                      String bizType, String bizNo, Long operatorId);

    void restock(Long tenantId, Long storeId, Long productId, int quantity,
                 String bizType, String bizNo, Long operatorId, String remark);
}
