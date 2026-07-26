package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.dto.V1MerchantStoreInventoryAdjustDTO;
import com.payment.dto.V1MerchantStoreInventoryLogVO;
import com.payment.dto.V1MerchantStoreInventoryVO;
import com.payment.entity.Product;
import com.payment.entity.Store;
import com.payment.entity.StoreInventoryChangeLog;
import com.payment.entity.StoreProductStock;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.StoreInventoryChangeLogMapper;
import com.payment.mapper.StoreMapper;
import com.payment.mapper.StoreProductStockMapper;
import com.payment.service.StoreInventoryService;
import com.payment.service.V1MerchantStoreInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商户端门店库存管理实现。
 */
@Service
@RequiredArgsConstructor
public class V1MerchantStoreInventoryServiceImpl implements V1MerchantStoreInventoryService {

    private final StoreProductStockMapper storeProductStockMapper;
    private final StoreInventoryChangeLogMapper inventoryChangeLogMapper;
    private final StoreMapper storeMapper;
    private final ProductMapper productMapper;
    private final StoreInventoryService storeInventoryService;
    private final V1MerchantSupportService v1MerchantSupportService;

    @Override
    public Page<V1MerchantStoreInventoryVO> listStocks(Long tenantId, Long platformUserId, Integer current, Integer size,
                                                       Long storeId, Long productId, Boolean lowStockOnly, Integer threshold) {
        requireManagePermission(tenantId, platformUserId);
        int safeThreshold = threshold == null ? 5 : Math.max(0, threshold);
        Page<StoreProductStock> page = storeProductStockMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<StoreProductStock>()
                        .eq(StoreProductStock::getTenantId, tenantId)
                        .eq(storeId != null, StoreProductStock::getStoreId, storeId)
                        .eq(productId != null, StoreProductStock::getProductId, productId)
                        .apply(Boolean.TRUE.equals(lowStockOnly), "quantity - locked_quantity <= {0}", safeThreshold)
                        .orderByAsc(StoreProductStock::getStoreId)
                        .orderByAsc(StoreProductStock::getProductId));
        Page<V1MerchantStoreInventoryVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toStockVO).toList());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantStoreInventoryVO adjustStock(Long tenantId, Long platformUserId,
                                                   V1MerchantStoreInventoryAdjustDTO dto) {
        requireManagePermission(tenantId, platformUserId);
        if (dto.getDelta() == null || dto.getDelta() == 0) {
            throw new BusinessException("库存调整数量不能为 0");
        }
        requireActiveStore(tenantId, dto.getStoreId());
        requireTenantProduct(tenantId, dto.getProductId());
        StoreProductStock stock = storeInventoryService.adjust(
                tenantId, dto.getStoreId(), dto.getProductId(), dto.getDelta(), platformUserId, dto.getRemark());
        return toStockVO(stock);
    }

    @Override
    public Page<V1MerchantStoreInventoryLogVO> listChangeLogs(Long tenantId, Long platformUserId, Integer current,
                                                               Integer size, Long storeId, Long productId) {
        requireManagePermission(tenantId, platformUserId);
        Page<StoreInventoryChangeLog> page = inventoryChangeLogMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<StoreInventoryChangeLog>()
                        .eq(StoreInventoryChangeLog::getTenantId, tenantId)
                        .eq(storeId != null, StoreInventoryChangeLog::getStoreId, storeId)
                        .eq(productId != null, StoreInventoryChangeLog::getProductId, productId)
                        .orderByDesc(StoreInventoryChangeLog::getCreateTime));
        Page<V1MerchantStoreInventoryLogVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toLogVO).toList());
        return result;
    }

    private void requireManagePermission(Long tenantId, Long platformUserId) {
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.INVENTORY_MANAGE);
    }

    private void requireActiveStore(Long tenantId, Long storeId) {
        Store store = storeMapper.selectOne(new LambdaQueryWrapper<Store>()
                .eq(Store::getId, storeId)
                .eq(Store::getTenantId, tenantId)
                .eq(Store::getDeleted, 0)
                .eq(Store::getStatus, 1));
        if (store == null) {
            throw new BusinessException("门店不存在或已停用");
        }
    }

    private void requireTenantProduct(Long tenantId, Long productId) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getDeleted, 0));
        if (product == null) {
            throw new BusinessException("商品不存在或不属于当前商户");
        }
    }

    private V1MerchantStoreInventoryVO toStockVO(StoreProductStock stock) {
        V1MerchantStoreInventoryVO vo = new V1MerchantStoreInventoryVO();
        vo.setId(stock.getId());
        vo.setTenantId(stock.getTenantId());
        vo.setStoreId(stock.getStoreId());
        vo.setProductId(stock.getProductId());
        vo.setQuantity(stock.getQuantity());
        vo.setLockedQuantity(stock.getLockedQuantity());
        vo.setAvailableQuantity(Math.max(0, valueOrZero(stock.getQuantity()) - valueOrZero(stock.getLockedQuantity())));
        vo.setUpdateTime(stock.getUpdateTime());

        Store store = storeMapper.selectById(stock.getStoreId());
        if (store != null) {
            vo.setStoreName(store.getStoreName());
        }
        Product product = productMapper.selectById(stock.getProductId());
        if (product != null) {
            vo.setProductCode(product.getProductCode());
            vo.setProductName(product.getName());
        }
        return vo;
    }

    private V1MerchantStoreInventoryLogVO toLogVO(StoreInventoryChangeLog log) {
        V1MerchantStoreInventoryLogVO vo = new V1MerchantStoreInventoryLogVO();
        vo.setId(log.getId());
        vo.setStoreId(log.getStoreId());
        vo.setProductId(log.getProductId());
        vo.setChangeType(log.getChangeType());
        vo.setChangeQuantity(log.getChangeQuantity());
        vo.setQuantityBefore(log.getQuantityBefore());
        vo.setQuantityAfter(log.getQuantityAfter());
        vo.setLockedBefore(log.getLockedBefore());
        vo.setLockedAfter(log.getLockedAfter());
        vo.setBizType(log.getBizType());
        vo.setBizNo(log.getBizNo());
        vo.setOperatorId(log.getOperatorId());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
