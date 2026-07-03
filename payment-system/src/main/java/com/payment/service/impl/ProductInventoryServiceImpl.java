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
 * 商品库存服务实现类。
 *
 * <p>负责支付成功后的库存扣减操作。通过 MyBatis-Plus 乐观锁（{@code @Version}）机制
 * 防止并发场景下的超卖问题，最多重试 {@value #MAX_RETRY_TIMES} 次以应对乐观锁冲突。</p>
 *
 * <p>多租户隔离由 MyBatis-Plus 全局拦截器 {@code TenantLineInnerInterceptor} 自动注入 {@code tenant_id} 条件，
 * 本类显式按 {@code tenantId + productId} 查询以确保业务语义清晰。</p>
 */
@Service
@RequiredArgsConstructor
public class ProductInventoryServiceImpl implements ProductInventoryService {

    /**
     * 最大重试次数，乐观锁冲突时重新读取并扣减。
     */
    private static final int MAX_RETRY_TIMES = 3;

    private final ProductStockMapper productStockMapper;

    /**
     * 扣减指定商品的库存。
     *
     * <p>流程：
     * <ol>
     *   <li>校验扣减数量必须大于 0；</li>
     *   <li>按租户 ID + 商品 ID 查询当前库存记录；</li>
     *   <li>校验库存记录存在且剩余库存充足；</li>
     *   <li>通过乐观锁更新库存（{@code @Version}），更新失败则重试；</li>
     *   <li>超过最大重试次数仍失败时抛出业务异常。</li>
     * </ol>
     *
     * @param tenantId  租户 ID，用于多租户库存隔离
     * @param productId 商品 ID
     * @param quantity  扣减数量，必须大于 0
     * @param bizNo     业务单号，用于异常日志追踪
     * @throws BusinessException 扣减数量非法、库存记录不存在、库存不足、或重试耗尽时抛出
     */
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
