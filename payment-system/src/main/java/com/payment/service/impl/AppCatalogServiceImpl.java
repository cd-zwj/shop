package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.entity.Product;
import com.payment.entity.Tenant;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.TenantMapper;
import com.payment.service.AppCatalogService;
import com.payment.service.ProductSearchService;
import com.payment.service.UserBehaviorLogService;
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
    private final ProductMapper productMapper;
    private final ProductSearchService productSearchService;
    private final UserBehaviorLogService userBehaviorLogService;

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
    public List<Product> listTenantProducts(Long tenantId) {
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getDeleted, 0)
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getCreateTime));
    }

    @Override
    public List<Product> searchTenantProducts(String keyword, Long tenantId) {
        return productSearchService.searchProducts(keyword, tenantId);
    }

    @Override
    public Product getProductAndRecordView(Long productId) {
        // C 端详情仅返回上架且未删除的商品；下架/删除商品返回 null，避免按 ID 绕过列表过滤
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getStatus, 1)
                .eq(Product::getDeleted, 0));
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
}
