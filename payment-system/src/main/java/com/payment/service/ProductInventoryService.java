package com.payment.service;

/**
 * 商品库存服务接口。
 *
 * <p>提供商品库存扣减能力，用于订单下单时锁定库存。
 * 使用乐观锁（version）保障并发扣减的正确性，配合幂等编号（bizNo）防止重复扣减。</p>
 */
public interface ProductInventoryService {

    /**
     * 扣减指定商品的库存。
     *
     * <p>在订单创建流程中调用，通过幂等编号防止重复扣减。
     * 库存不足时抛出业务异常。</p>
     *
     * @param tenantId  租户ID
     * @param productId 商品ID
     * @param quantity  扣减数量（正整数）
     * @param bizNo     业务单号（幂等键，防止重复扣减）
     * @throws com.payment.common.exception.BusinessException 当库存不足或bizNo重复时抛出
     */
    void deductStock(Long tenantId, Long productId, Integer quantity, String bizNo);
}
