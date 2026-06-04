package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.AppCatalogProductSearchQueryDTO;
import com.payment.dto.AppCatalogSearchProductVO;
import com.payment.dto.AppCatalogSearchTenantVO;
import com.payment.dto.AppCatalogTenantSearchQueryDTO;
import com.payment.entity.Product;
import com.payment.entity.Tenant;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.TenantMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户端公开搜索服务测试。
 */
class AppCatalogSearchServiceImplTest {

    @BeforeAll
    static void initMybatisPlusMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Product.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Tenant.class);
    }

    @Test
    void searchProductsShouldReturnDefaultPageWhenKeywordIsBlank() {
        ProductMapper productMapper = mock(ProductMapper.class);
        TenantMapper tenantMapper = mock(TenantMapper.class);
        AppCatalogSearchServiceImpl service = new AppCatalogSearchServiceImpl(productMapper, tenantMapper);
        Product product = product(1L, 10L, "拿铁", "饮品");
        Tenant tenant = tenant(10L, "晨光咖啡");
        Page<Product> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(product));

        when(productMapper.selectPage(any(), any())).thenReturn(page);
        when(tenantMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(tenant));

        AppCatalogProductSearchQueryDTO query = new AppCatalogProductSearchQueryDTO();
        query.setKeyword("   ");

        Page<AppCatalogSearchProductVO> result = service.searchProducts(query);

        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(1, result.getTotal());
        assertEquals("拿铁", result.getRecords().get(0).getName());
        assertEquals("晨光咖啡", result.getRecords().get(0).getTenantName());
        verify(productMapper).selectPage(any(), any());
    }

    @Test
    void searchProductsShouldReturnEmptyPageWhenNoResult() {
        ProductMapper productMapper = mock(ProductMapper.class);
        TenantMapper tenantMapper = mock(TenantMapper.class);
        AppCatalogSearchServiceImpl service = new AppCatalogSearchServiceImpl(productMapper, tenantMapper);
        Page<Product> page = new Page<>(1, 10, 0);
        page.setRecords(List.of());

        when(productMapper.selectPage(any(), any())).thenReturn(page);

        AppCatalogProductSearchQueryDTO query = new AppCatalogProductSearchQueryDTO();
        query.setKeyword("不存在商品");

        Page<AppCatalogSearchProductVO> result = service.searchProducts(query);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void searchProductsShouldNormalizeOutOfRangePaginationAndInvalidSort() {
        ProductMapper productMapper = mock(ProductMapper.class);
        TenantMapper tenantMapper = mock(TenantMapper.class);
        AppCatalogSearchServiceImpl service = new AppCatalogSearchServiceImpl(productMapper, tenantMapper);
        Page<Product> page = new Page<>(1, 50, 0);
        page.setRecords(List.of());

        when(productMapper.selectPage(any(), any())).thenReturn(page);

        AppCatalogProductSearchQueryDTO query = new AppCatalogProductSearchQueryDTO();
        query.setCurrent(-9);
        query.setSize(999);
        query.setSort("DROP_TABLE");

        Page<AppCatalogSearchProductVO> result = service.searchProducts(query);

        assertEquals(1, result.getCurrent());
        assertEquals(50, result.getSize());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void searchTenantsShouldSupportKeywordAndReturnUnifiedCardFields() {
        ProductMapper productMapper = mock(ProductMapper.class);
        TenantMapper tenantMapper = mock(TenantMapper.class);
        AppCatalogSearchServiceImpl service = new AppCatalogSearchServiceImpl(productMapper, tenantMapper);
        Tenant tenant = tenant(20L, "城市便利店");
        tenant.setAddress("湖滨商圈");
        Page<Tenant> tenantPage = new Page<>(1, 10, 1);
        tenantPage.setRecords(List.of(tenant));

        when(tenantMapper.selectPage(any(), any())).thenReturn(tenantPage);
        when(productMapper.selectMaps(any())).thenReturn(List.of(Map.of("tenant_id", 20L, "product_count", 3L)));

        AppCatalogTenantSearchQueryDTO query = new AppCatalogTenantSearchQueryDTO();
        query.setKeyword("便利");
        query.setCategory("全部分类");
        query.setSort("name_asc");

        Page<AppCatalogSearchTenantVO> result = service.searchTenants(query);

        assertEquals(1, result.getTotal());
        assertEquals("城市便利店", result.getRecords().get(0).getTitle());
        assertEquals("湖滨商圈", result.getRecords().get(0).getAddress());
        assertEquals(3L, result.getRecords().get(0).getProductCount());
        verify(tenantMapper).selectPage(any(), any());
        verify(tenantMapper, never()).selectList(any());
        verify(productMapper, times(1)).selectMaps(any());
        verify(productMapper, never()).selectCount(any());
    }

    @Test
    void searchTenantsShouldReturnEmptyPageForNoResultAndTolerateUnsupportedFilters() {
        ProductMapper productMapper = mock(ProductMapper.class);
        TenantMapper tenantMapper = mock(TenantMapper.class);
        AppCatalogSearchServiceImpl service = new AppCatalogSearchServiceImpl(productMapper, tenantMapper);

        Page<Tenant> tenantPage = new Page<>(1, 10, 0);
        tenantPage.setRecords(List.of());

        when(tenantMapper.selectPage(any(), any())).thenReturn(tenantPage);

        AppCatalogTenantSearchQueryDTO query = new AppCatalogTenantSearchQueryDTO();
        query.setKeyword("不存在商户");
        query.setMaxDistanceKm(-1);
        query.setSort("unknown_sort");

        Page<AppCatalogSearchTenantVO> result = service.searchTenants(query);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void searchTenantsShouldBatchProductCountsForReturnedPage() {
        ProductMapper productMapper = mock(ProductMapper.class);
        TenantMapper tenantMapper = mock(TenantMapper.class);
        AppCatalogSearchServiceImpl service = new AppCatalogSearchServiceImpl(productMapper, tenantMapper);
        Tenant first = tenant(21L, "第一家");
        Tenant second = tenant(22L, "第二家");
        Page<Tenant> tenantPage = new Page<>(1, 10, 2);
        tenantPage.setRecords(List.of(first, second));

        when(tenantMapper.selectPage(any(), any())).thenReturn(tenantPage);
        when(productMapper.selectMaps(any())).thenReturn(List.of(
                Map.of("tenant_id", 21L, "product_count", 4L),
                Map.of("tenant_id", 22L, "product_count", 7L)
        ));

        Page<AppCatalogSearchTenantVO> result = service.searchTenants(new AppCatalogTenantSearchQueryDTO());

        assertEquals(4L, result.getRecords().get(0).getProductCount());
        assertEquals(7L, result.getRecords().get(1).getProductCount());
        verify(productMapper, times(1)).selectMaps(any());
        verify(productMapper, never()).selectCount(any());
    }

    @Test
    void searchTenantsShouldApplyStoreFiltersAndReturnDistanceRatingAndCategory() {
        ProductMapper productMapper = mock(ProductMapper.class);
        TenantMapper tenantMapper = mock(TenantMapper.class);
        AppCatalogSearchServiceImpl service = new AppCatalogSearchServiceImpl(productMapper, tenantMapper);
        AppCatalogSearchTenantVO tenant = tenantSearchVO(30L, "湖滨轻食");
        tenant.setAddress("湖滨银泰A座");
        tenant.setCategory("餐饮,轻食");
        tenant.setRating(new BigDecimal("4.80"));
        tenant.setDistanceLabel("0.01km");
        Page<AppCatalogSearchTenantVO> tenantPage = new Page<>(1, 10, 1);
        tenantPage.setRecords(List.of(tenant));

        when(tenantMapper.selectSearchTenantPage(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(tenantPage);
        when(productMapper.selectMaps(any())).thenReturn(List.of(Map.of("tenant_id", 30L, "product_count", 2L)));

        AppCatalogTenantSearchQueryDTO query = new AppCatalogTenantSearchQueryDTO();
        query.setCategory("餐饮");
        query.setRegion("湖滨");
        query.setMinRating(new BigDecimal("4.5"));
        query.setMaxDistanceKm(5);
        query.setLatitude(new BigDecimal("30.250100"));
        query.setLongitude(new BigDecimal("120.160100"));
        query.setSort("distance");

        Page<AppCatalogSearchTenantVO> result = service.searchTenants(query);

        assertEquals(1, result.getTotal());
        assertEquals("湖滨轻食", result.getRecords().get(0).getTitle());
        assertEquals("餐饮,轻食", result.getRecords().get(0).getCategory());
        assertEquals(new BigDecimal("4.80"), result.getRecords().get(0).getRating());
        assertTrue(result.getRecords().get(0).getDistanceLabel().endsWith("km"));
        assertEquals(2L, result.getRecords().get(0).getProductCount());
        verify(tenantMapper, never()).selectList(any());
        verify(tenantMapper, times(1))
                .selectSearchTenantPage(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                        anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
        verify(productMapper, times(1)).selectMaps(any());
        verify(productMapper, never()).selectCount(any());
    }

    @Test
    void searchTenantsShouldPageStoreSortedResultsInDatabase() {
        ProductMapper productMapper = mock(ProductMapper.class);
        TenantMapper tenantMapper = mock(TenantMapper.class);
        AppCatalogSearchServiceImpl service = new AppCatalogSearchServiceImpl(productMapper, tenantMapper);
        AppCatalogSearchTenantVO first = tenantSearchVO(41L, "高分门店");
        first.setRating(new BigDecimal("4.90"));
        Page<AppCatalogSearchTenantVO> tenantPage = new Page<>(2, 5, 18);
        tenantPage.setRecords(List.of(first));

        when(tenantMapper.selectSearchTenantPage(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(tenantPage);
        when(productMapper.selectMaps(any())).thenReturn(List.of(Map.of("tenant_id", 41L, "product_count", 6L)));

        AppCatalogTenantSearchQueryDTO query = new AppCatalogTenantSearchQueryDTO();
        query.setCurrent(2);
        query.setSize(5);
        query.setSort("rating_desc");

        Page<AppCatalogSearchTenantVO> result = service.searchTenants(query);

        assertEquals(2, result.getCurrent());
        assertEquals(5, result.getSize());
        assertEquals(18, result.getTotal());
        assertEquals(6L, result.getRecords().get(0).getProductCount());
        verify(tenantMapper, never()).selectList(any());
        verify(tenantMapper, times(1))
                .selectSearchTenantPage(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                        anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void searchProductsShouldEscapeLikeWildcardsInKeyword() {
        ProductMapper productMapper = mock(ProductMapper.class);
        TenantMapper tenantMapper = mock(TenantMapper.class);
        AppCatalogSearchServiceImpl service = new AppCatalogSearchServiceImpl(productMapper, tenantMapper);
        Page<Product> page = new Page<>(1, 10, 0);
        page.setRecords(List.of());
        ArgumentCaptor<LambdaQueryWrapper<Product>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);

        when(productMapper.selectPage(any(), any())).thenReturn(page);

        AppCatalogProductSearchQueryDTO query = new AppCatalogProductSearchQueryDTO();
        query.setKeyword("100%_拿铁");

        service.searchProducts(query);

        verify(productMapper).selectPage(any(), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        String params = wrapperCaptor.getValue().getParamNameValuePairs().values().toString();
        assertTrue(sqlSegment.contains("ESCAPE"));
        assertTrue(params.contains("100\\%\\_拿铁"));
    }

    private Product product(Long id, Long tenantId, String name, String category) {
        Product product = new Product();
        product.setId(id);
        product.setTenantId(tenantId);
        product.setName(name);
        product.setCategory(category);
        product.setPrice(new BigDecimal("12.50"));
        product.setStatus(1);
        product.setDeleted(0);
        return product;
    }

    private Tenant tenant(Long id, String name) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName(name);
        tenant.setStatus(1);
        tenant.setDeleted(0);
        return tenant;
    }

    private AppCatalogSearchTenantVO tenantSearchVO(Long id, String name) {
        AppCatalogSearchTenantVO vo = new AppCatalogSearchTenantVO();
        vo.setId(id);
        vo.setTenantId(id);
        vo.setTitle(name);
        vo.setName(name);
        vo.setStatus(1);
        return vo;
    }
}
