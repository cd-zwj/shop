package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.entity.Product;
import com.payment.entity.Store;
import com.payment.entity.Tenant;
import com.payment.mapper.StoreMapper;
import com.payment.mapper.StoreProductMapper;
import com.payment.mapper.TenantMapper;
import com.payment.service.AppCatalogService;
import com.payment.service.UserBehaviorLogService;
import com.payment.util.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户端商户与商品浏览服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppCatalogServiceImpl implements AppCatalogService {

    private final TenantMapper tenantMapper;
    private final UserBehaviorLogService userBehaviorLogService;
    private final StoreMapper storeMapper;
    private final StoreProductMapper storeProductMapper;

    @Override
    public List<Tenant> listActiveTenants() {
        return tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getStatus, 1)
                .eq(Tenant::getDeleted, 0)
                .orderByDesc(Tenant::getCreateTime));
    }

    @Override
    public Tenant getTenant(Long tenantId) {
        // C 端详情仅返回启用且未删除的商户，避免按 ID 绕过列表过滤
        return tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getId, tenantId)
                .eq(Tenant::getStatus, 1)
                .eq(Tenant::getDeleted, 0));
    }

    @Override
    public List<Store> listActiveTenantStores(Long tenantId) {
        return withTenantContext(tenantId, () -> storeMapper.selectList(new LambdaQueryWrapper<Store>()
                .eq(Store::getTenantId, tenantId)
                .eq(Store::getStatus, 1)
                .eq(Store::getDeleted, 0)
                .orderByDesc(Store::getCreateTime)));
    }

    @Override
    public List<Product> listTenantProducts(Long tenantId, Long storeId) {
        validateActiveStore(tenantId, storeId);
        return storeProductMapper.selectVisibleProductsByStore(tenantId, storeId);
    }

    @Override
    public Product getProductAndRecordView(Long productId, Long storeId) {
        if (storeId == null || storeId <= 0) {
            return null;
        }
        Product product = storeProductMapper.selectVisibleProductByStore(null, productId, storeId);
        // 记录商品浏览行为（埋点失败不影响主流程）
        try {
            if (product != null) {
                userBehaviorLogService.recordBehavior(
                        null, product.getTenantId(), "VIEW",
                        "PRODUCT", productId, null);
            }
        } catch (Exception e) {
            log.warn("记录 VIEW 行为日志失败, productId={}", productId, e);
        }
        return product;
    }

    private void validateActiveStore(Long tenantId, Long storeId) {
        if (storeId == null || storeId <= 0) {
            throw new com.payment.common.BusinessException("请选择自提门店");
        }
        Store store = storeMapper.selectOne(new LambdaQueryWrapper<Store>()
                .eq(Store::getId, storeId)
                .eq(Store::getTenantId, tenantId)
                .eq(Store::getStatus, 1)
                .eq(Store::getDeleted, 0));
        if (store == null) {
            throw new com.payment.common.BusinessException("门店不存在或已停业");
        }
    }

    private <T> T withTenantContext(Long tenantId, java.util.function.Supplier<T> supplier) {
        Long previousTenantId = TenantContextHolder.getTenantId();
        try {
            TenantContextHolder.setTenantId(tenantId);
            return supplier.get();
        } finally {
            TenantContextHolder.clear();
            if (previousTenantId != null) {
                TenantContextHolder.setTenantId(previousTenantId);
            }
        }
    }
}
