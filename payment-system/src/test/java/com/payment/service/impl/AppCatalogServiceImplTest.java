package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.entity.Product;
import com.payment.entity.Tenant;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.TenantMapper;
import com.payment.service.ProductSearchService;
import com.payment.service.UserBehaviorLogService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户端商户与商品浏览服务测试。
 */
class AppCatalogServiceImplTest {

    private final TenantMapper tenantMapper = mock(TenantMapper.class);
    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final ProductSearchService productSearchService = mock(ProductSearchService.class);
    private final UserBehaviorLogService userBehaviorLogService = mock(UserBehaviorLogService.class);

    private final AppCatalogServiceImpl service = new AppCatalogServiceImpl(
            tenantMapper, productMapper, productSearchService, userBehaviorLogService);

    @Test
    void listActiveTenantsShouldDelegateToMapper() {
        List<Tenant> tenants = List.of(buildTenant(1L), buildTenant(2L));
        when(tenantMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(tenants);

        List<Tenant> result = service.listActiveTenants();

        assertEquals(2, result.size());
    }

    @Test
    void getTenantShouldDelegateToMapper() {
        Tenant tenant = buildTenant(1L);
        when(tenantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(tenant);

        assertSame(tenant, service.getTenant(1L));
    }

    @Test
    void getTenantShouldReturnNullWhenInactiveOrDeleted() {
        // status=0 / deleted=1 的商户不应通过详情接口暴露给 C 端
        when(tenantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertNull(service.getTenant(999L));
    }

    @Test
    void listTenantProductsShouldDelegateToMapper() {
        when(productMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(buildProduct(1L, 1L)));

        assertEquals(1, service.listTenantProducts(1L).size());
    }

    @Test
    void searchTenantProductsShouldDelegateToSearchService() {
        when(productSearchService.searchProducts("coffee", 1L))
                .thenReturn(List.of(buildProduct(1L, 1L)));

        List<Product> result = service.searchTenantProducts("coffee", 1L);

        assertEquals(1, result.size());
        verify(productSearchService).searchProducts("coffee", 1L);
    }

    @Test
    void getProductShouldRecordViewBehavior() {
        Product product = buildProduct(5L, 9L);
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(product);

        Product result = service.getProductAndRecordView(5L);

        assertSame(product, result);
        verify(userBehaviorLogService).recordBehavior(
                isNull(), eq(9L), eq("VIEW"), eq("PRODUCT"), eq(5L), isNull());
    }

    @Test
    void getProductShouldSkipBehaviorWhenProductMissing() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertNull(service.getProductAndRecordView(404L));
        verify(userBehaviorLogService, never())
                .recordBehavior(any(), any(), any(), any(), any(), any());
    }

    @Test
    void getProductShouldSwallowBehaviorLogFailure() {
        Product product = buildProduct(5L, 9L);
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(product);
        doThrow(new RuntimeException("埋点异常")).when(userBehaviorLogService)
                .recordBehavior(any(), any(), any(), any(), any(), any());

        // 埋点失败不应影响商品详情返回
        assertSame(product, service.getProductAndRecordView(5L));
    }

    @Test
    void getProductShouldReturnNullWhenInactiveOrDeleted() {
        // status=0 / deleted=1 的商品不应通过详情接口暴露给 C 端
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertNull(service.getProductAndRecordView(404L));
        verify(userBehaviorLogService, never())
                .recordBehavior(any(), any(), any(), any(), any(), any());
    }

    private Tenant buildTenant(Long id) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setStatus(1);
        tenant.setDeleted(0);
        return tenant;
    }

    private Product buildProduct(Long id, Long tenantId) {
        Product product = new Product();
        product.setId(id);
        product.setTenantId(tenantId);
        product.setStatus(1);
        product.setDeleted(0);
        return product;
    }
}
