package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.entity.ProductStock;
import com.payment.mapper.ProductStockMapper;
import com.payment.service.ProductInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品库存服务实现。
 *
 * 库存在支付成功后扣减，使用乐观锁重试避免并发超卖。
 */
@Service
@RequiredArgsConstructor
public class ProductInventoryServiceImpl implements ProductInventoryService {

    private static final int MAX_RETRY_TIMES = 3;

    private final ProductStockMapper productStockMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(Long tenantId, Long productId, Integer quantity, String bizNo) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("库存扣减数量必须大于0");
        }

        for (int retry = 0; retry < MAX_RETRY_TIMES; retry++) {
            ProductStock productStock = productStockMapper.selectOne(new LambdaQueryWrapper<ProductStock>()
                    .eq(ProductStock::getTenantId, tenantId)
                    .eq(ProductStock::getProductId, productId));

            if (productStock == null) {
                throw new BusinessException("库存记录不存在, productId=" + productId + ", bizNo=" + bizNo);
            }
            if (productStock.getQuantity() < quantity) {
                throw new BusinessException("库存不足, productId=" + productId + ", bizNo=" + bizNo);
            }

            productStock.setQuantity(productStock.getQuantity() - quantity);
            int updatedRows = productStockMapper.updateById(productStock);
            if (updatedRows == 1) {
                return;
            }
        }

        throw new BusinessException("库存扣减失败，请稍后重试, productId=" + productId + ", bizNo=" + bizNo);
    }
}
