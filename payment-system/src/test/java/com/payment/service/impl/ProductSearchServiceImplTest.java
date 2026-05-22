package com.payment.service.impl;

import com.payment.document.ProductDocument;
import com.payment.entity.Product;
import com.payment.mapper.ProductMapper;
import com.payment.repository.ProductRepository;
import com.payment.util.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductSearchServiceImplTest {

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
}
