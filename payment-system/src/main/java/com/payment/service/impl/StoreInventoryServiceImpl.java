package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.entity.StoreInventoryChangeLog;
import com.payment.entity.StoreProductStock;
import com.payment.mapper.StoreInventoryChangeLogMapper;
import com.payment.mapper.StoreProductStockMapper;
import com.payment.service.StoreInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 门店库存账本实现。
 *
 * <p>库存行使用 {@code SELECT ... FOR UPDATE} 串行化，库存流水以
 * {@code store + product + changeType + bizType + bizNo} 去重，支付回调重投不会重复扣减。</p>
 */
@Service
@RequiredArgsConstructor
public class StoreInventoryServiceImpl implements StoreInventoryService {

    private final StoreProductStockMapper storeProductStockMapper;
    private final StoreInventoryChangeLogMapper inventoryChangeLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoreProductStock getOrCreate(Long tenantId, Long storeId, Long productId) {
        validateIdentity(tenantId, storeId, productId);
        return getOrCreateForUpdate(tenantId, storeId, productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoreProductStock adjust(Long tenantId, Long storeId, Long productId, int delta,
                                    Long operatorId, String remark) {
        if (delta == 0) {
            throw new BusinessException("库存调整数量不能为 0");
        }
        StoreProductStock stock = getOrCreateForUpdate(tenantId, storeId, productId);
        return mutate(stock, delta, 0, "ADJUST", null, null, operatorId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lock(Long tenantId, Long storeId, Long productId, int quantity, String bizType, String bizNo) {
        validateQuantityAndBiz(tenantId, storeId, productId, quantity, bizType, bizNo);
        if (alreadyRecorded(tenantId, storeId, productId, "LOCK", bizType, bizNo)) {
            return;
        }
        StoreProductStock stock = getOrCreateForUpdate(tenantId, storeId, productId);
        mutate(stock, 0, quantity, "LOCK", bizType, bizNo, null, "订单库存锁定");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(Long tenantId, Long storeId, Long productId, int quantity, String bizType, String bizNo) {
        validateQuantityAndBiz(tenantId, storeId, productId, quantity, bizType, bizNo);
        if (alreadyRecorded(tenantId, storeId, productId, "RELEASE", bizType, bizNo)) {
            return;
        }
        StoreProductStock stock = getOrCreateForUpdate(tenantId, storeId, productId);
        mutate(stock, 0, -quantity, "RELEASE", bizType, bizNo, null, "订单库存释放");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductLocked(Long tenantId, Long storeId, Long productId, int quantity,
                             String bizType, String bizNo, Long operatorId) {
        validateQuantityAndBiz(tenantId, storeId, productId, quantity, bizType, bizNo);
        if (alreadyRecorded(tenantId, storeId, productId, "DEDUCT_LOCKED", bizType, bizNo)) {
            return;
        }
        StoreInventoryChangeLog lockRecord = findRecord(tenantId, storeId, productId, "LOCK", bizType, bizNo);
        if (lockRecord == null
                || amountOrZero(lockRecord.getLockedAfter()) - amountOrZero(lockRecord.getLockedBefore()) < quantity) {
            throw new BusinessException("订单未锁定足够的门店库存");
        }
        StoreProductStock stock = getOrCreateForUpdate(tenantId, storeId, productId);
        mutate(stock, -quantity, -quantity, "DEDUCT_LOCKED", bizType, bizNo, operatorId, "已锁定订单销售扣减");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restock(Long tenantId, Long storeId, Long productId, int quantity,
                        String bizType, String bizNo, Long operatorId, String remark) {
        validateQuantityAndBiz(tenantId, storeId, productId, quantity, bizType, bizNo);
        if (alreadyRecorded(tenantId, storeId, productId, "RESTOCK", bizType, bizNo)) {
            return;
        }
        StoreProductStock stock = getOrCreateForUpdate(tenantId, storeId, productId);
        mutate(stock, quantity, 0, "RESTOCK", bizType, bizNo, operatorId,
                remark == null || remark.isBlank() ? "退货入库" : remark);
    }

    private StoreProductStock getOrCreateForUpdate(Long tenantId, Long storeId, Long productId) {
        validateIdentity(tenantId, storeId, productId);
        StoreProductStock stock = storeProductStockMapper.selectForUpdate(tenantId, storeId, productId);
        if (stock != null) {
            return stock;
        }

        StoreProductStock created = new StoreProductStock();
        created.setTenantId(tenantId);
        created.setStoreId(storeId);
        created.setProductId(productId);
        created.setQuantity(0);
        created.setLockedQuantity(0);
        created.setVersion(0);
        try {
            storeProductStockMapper.insert(created);
        } catch (DuplicateKeyException ignored) {
            // 另一事务已完成初始化，下面重新加锁读取即可。
        }

        stock = storeProductStockMapper.selectForUpdate(tenantId, storeId, productId);
        if (stock == null) {
            throw new BusinessException("门店库存初始化失败，请稍后重试");
        }
        return stock;
    }

    private StoreProductStock mutate(StoreProductStock stock,
                                     int quantityDelta,
                                     int lockedDelta,
                                     String changeType,
                                     String bizType,
                                     String bizNo,
                                     Long operatorId,
                                     String remark) {
        int beforeQuantity = amountOrZero(stock.getQuantity());
        int beforeLocked = amountOrZero(stock.getLockedQuantity());
        int afterQuantity = beforeQuantity + quantityDelta;
        int afterLocked = beforeLocked + lockedDelta;
        if (afterQuantity < 0) {
            throw new BusinessException("门店库存不足");
        }
        if (afterLocked < 0) {
            throw new BusinessException("门店锁定库存不足");
        }
        if (afterLocked > afterQuantity) {
            throw new BusinessException("可用门店库存不足");
        }

        stock.setQuantity(afterQuantity);
        stock.setLockedQuantity(afterLocked);
        if (storeProductStockMapper.updateById(stock) != 1) {
            throw new BusinessException("门店库存更新失败，请稍后重试");
        }

        StoreInventoryChangeLog log = new StoreInventoryChangeLog();
        log.setTenantId(stock.getTenantId());
        log.setStoreId(stock.getStoreId());
        log.setProductId(stock.getProductId());
        log.setChangeType(changeType);
        log.setChangeQuantity(quantityDelta);
        log.setQuantityBefore(beforeQuantity);
        log.setQuantityAfter(afterQuantity);
        log.setLockedBefore(beforeLocked);
        log.setLockedAfter(afterLocked);
        log.setBizType(bizType);
        log.setBizNo(bizNo);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        inventoryChangeLogMapper.insert(log);
        return stock;
    }

    private boolean alreadyRecorded(Long tenantId, Long storeId, Long productId,
                                    String changeType, String bizType, String bizNo) {
        return inventoryChangeLogMapper.selectCount(recordQuery(tenantId, storeId, productId, changeType, bizType, bizNo)) > 0;
    }

    private StoreInventoryChangeLog findRecord(Long tenantId, Long storeId, Long productId,
                                                String changeType, String bizType, String bizNo) {
        return inventoryChangeLogMapper.selectOne(recordQuery(tenantId, storeId, productId, changeType, bizType, bizNo));
    }

    private LambdaQueryWrapper<StoreInventoryChangeLog> recordQuery(Long tenantId, Long storeId, Long productId,
                                                                      String changeType, String bizType, String bizNo) {
        return new LambdaQueryWrapper<StoreInventoryChangeLog>()
                .eq(StoreInventoryChangeLog::getTenantId, tenantId)
                .eq(StoreInventoryChangeLog::getStoreId, storeId)
                .eq(StoreInventoryChangeLog::getProductId, productId)
                .eq(StoreInventoryChangeLog::getChangeType, changeType)
                .eq(StoreInventoryChangeLog::getBizType, bizType)
                .eq(StoreInventoryChangeLog::getBizNo, bizNo);
    }

    private void validateQuantityAndBiz(Long tenantId, Long storeId, Long productId, int quantity,
                                        String bizType, String bizNo) {
        validateIdentity(tenantId, storeId, productId);
        if (quantity <= 0) {
            throw new BusinessException("库存数量必须大于 0");
        }
        if (bizType == null || bizType.isBlank() || bizNo == null || bizNo.isBlank()) {
            throw new BusinessException("库存业务类型和业务单号不能为空");
        }
    }

    private void validateIdentity(Long tenantId, Long storeId, Long productId) {
        if (tenantId == null || tenantId <= 0 || storeId == null || storeId <= 0 || productId == null || productId <= 0) {
            throw new BusinessException("门店库存参数不合法");
        }
    }

    private int amountOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
