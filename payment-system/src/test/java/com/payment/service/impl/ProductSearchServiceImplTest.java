package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.payment.document.ProductDocument;
import com.payment.entity.Product;
import com.payment.mapper.ProductMapper;
import com.payment.repository.ProductRepository;
import com.payment.util.TenantContextHolder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductSearchServiceImplTest {

    @BeforeAll
    static void initMybatisPlusMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                Product.class
        );
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void searchProductsShouldMapSearchHitsToProducts() {
        ProductSearchServiceImpl service = new ProductSearchServiceImpl();
        ProductMapper productMapper = mock(ProductMapper.class);
        ElasticsearchOperations elasticsearchOperations = mock(ElasticsearchOperations.class);
        SearchHits<ProductDocument> searchHits = mock(SearchHits.class);
        SearchHit<ProductDocument> searchHit = mock(SearchHit.class);

        ProductDocument document = new ProductDocument();
        document.setId(88L);
        document.setTenantId(99L);
        document.setName("橙汁");
        document.setPrice(new BigDecimal("5.20"));
        document.setStatus(1);

        ReflectionTestUtils.setField(service, "productMapper", productMapper);
        ReflectionTestUtils.setField(service, "elasticsearchOperations", elasticsearchOperations);
        ReflectionTestUtils.setField(service, "productRepository", mock(ProductRepository.class));
        TenantContextHolder.setTenantId(99L);

        when(searchHit.getContent()).thenReturn(document);
        when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(ProductDocument.class))).thenReturn(searchHits);

        List<Product> result = service.searchProducts("橙", null);

        assertEquals(1, result.size());
        assertEquals(88L, result.get(0).getId());
        assertEquals("橙汁", result.get(0).getName());
        assertEquals(0, result.get(0).getDeleted());
    }

    @Test
    void searchProductsShouldFallbackToDatabaseQueryWhenElasticsearchFails() {
        ProductSearchServiceImpl service = new ProductSearchServiceImpl();
        ProductMapper productMapper = mock(ProductMapper.class);
        ElasticsearchOperations elasticsearchOperations = mock(ElasticsearchOperations.class);
        Product product = new Product();
        product.setId(123L);
        product.setTenantId(321L);
        product.setName("牛奶");

        ReflectionTestUtils.setField(service, "productMapper", productMapper);
        ReflectionTestUtils.setField(service, "elasticsearchOperations", elasticsearchOperations);
        ReflectionTestUtils.setField(service, "productRepository", mock(ProductRepository.class));
        TenantContextHolder.setTenantId(321L);

        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(ProductDocument.class)))
                .thenThrow(new RuntimeException("es unavailable"));
        when(productMapper.selectList(any())).thenReturn(List.of(product));

        List<Product> result = service.searchProducts("牛", null);

        assertEquals(1, result.size());
        assertEquals(123L, result.get(0).getId());
        verify(productMapper).selectList(any());
    }

    @Test
    void searchProductsShouldFallbackToDatabaseWhenElasticsearchDisabled() {
        // 模拟 app.search.enabled=false：ElasticsearchOperations 与 ProductRepository 均未装配
        ProductSearchServiceImpl service = new ProductSearchServiceImpl();
        ProductMapper productMapper = mock(ProductMapper.class);
        Product product = new Product();
        product.setId(7L);
        product.setTenantId(42L);
        product.setName("可乐");

        ReflectionTestUtils.setField(service, "productMapper", productMapper);
        ReflectionTestUtils.setField(service, "elasticsearchOperations", null);
        ReflectionTestUtils.setField(service, "productRepository", null);
        TenantContextHolder.setTenantId(42L);

        when(productMapper.selectList(any())).thenReturn(List.of(product));

        List<Product> result = service.searchProducts("可", null);

        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getId());
        // ES 关闭时应直接走 DB，不应触碰 ElasticsearchOperations
        verify(productMapper).selectList(any());
    }

    @Test
    void searchProductsDatabaseFallbackShouldFilterByStatusActive() {
        // 与 ES 路径（searchProducts 含 status=1）口径一致：DB 降级也应只返回上架商品
        ProductSearchServiceImpl service = new ProductSearchServiceImpl();
        ProductMapper productMapper = mock(ProductMapper.class);
        ReflectionTestUtils.setField(service, "productMapper", productMapper);
        ReflectionTestUtils.setField(service, "elasticsearchOperations", null);
        ReflectionTestUtils.setField(service, "productRepository", null);
        TenantContextHolder.setTenantId(42L);
        when(productMapper.selectList(any())).thenReturn(List.of());

        service.searchProducts("可", null);

        ArgumentCaptor<LambdaQueryWrapper<Product>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(productMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        // 仅校验 status 条件存在；MyBatis-Plus 占位符为 #{ew.paramNameValuePairs....}，因此只断言列名而非具体值
        assertTrue(sqlSegment.contains("status"),
                "DB 降级搜索应当包含 status=1 过滤，实际 SQL 段: " + sqlSegment);
        assertTrue(sqlSegment.contains("deleted"),
                "DB 降级搜索应当过滤 deleted=0，实际 SQL 段: " + sqlSegment);
    }

    @Test
    void searchByCategoryDatabaseFallbackShouldNotFilterByStatus() {
        // ES 路径下 searchByCategory 不过滤 status；DB 降级应保持一致，否则会出现"开 ES 看得到，关 ES 看不到"
        ProductSearchServiceImpl service = new ProductSearchServiceImpl();
        ProductMapper productMapper = mock(ProductMapper.class);
        ReflectionTestUtils.setField(service, "productMapper", productMapper);
        ReflectionTestUtils.setField(service, "elasticsearchOperations", null);
        ReflectionTestUtils.setField(service, "productRepository", null);
        TenantContextHolder.setTenantId(42L);
        when(productMapper.selectList(any())).thenReturn(List.of());

        service.searchByCategory("饮品");

        ArgumentCaptor<LambdaQueryWrapper<Product>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(productMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("category"),
                "DB 降级分类搜索应当包含 category 过滤，实际 SQL 段: " + sqlSegment);
        // 确认未注入 status 条件
        assertTrue(!sqlSegment.contains("status"),
                "DB 降级分类搜索不应过滤 status（与 ES 路径保持一致），实际 SQL 段: " + sqlSegment);
    }

    @Test
    void searchByCategoryShouldFallbackToDatabaseWhenElasticsearchDisabled() {
        ProductSearchServiceImpl service = new ProductSearchServiceImpl();
        ProductMapper productMapper = mock(ProductMapper.class);
        Product product = new Product();
        product.setId(11L);
        product.setTenantId(42L);
        product.setCategory("饮品");

        ReflectionTestUtils.setField(service, "productMapper", productMapper);
        ReflectionTestUtils.setField(service, "elasticsearchOperations", null);
        ReflectionTestUtils.setField(service, "productRepository", null);
        TenantContextHolder.setTenantId(42L);

        when(productMapper.selectList(any())).thenReturn(List.of(product));

        List<Product> result = service.searchByCategory("饮品");

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getId());
        verify(productMapper).selectList(any());
    }

    @Test
    void syncProductShouldSkipWhenRepositoryDisabled() {
        // app.search.enabled=false 时 productRepository 为 null，索引同步应静默跳过
        ProductSearchServiceImpl service = new ProductSearchServiceImpl();
        ProductMapper productMapper = mock(ProductMapper.class);
        ReflectionTestUtils.setField(service, "productMapper", productMapper);
        ReflectionTestUtils.setField(service, "productRepository", null);
        ReflectionTestUtils.setField(service, "elasticsearchOperations", null);

        Product product = new Product();
        product.setId(99L);

        // 不应抛异常
        service.syncProduct(product);
        service.deleteProduct(99L);
    }
}
