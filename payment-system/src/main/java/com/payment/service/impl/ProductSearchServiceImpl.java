package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.document.ProductDocument;
import com.payment.entity.Product;
import com.payment.repository.ProductRepository;
import com.payment.service.ProductSearchService;
import com.payment.service.ProductService;
import com.payment.util.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.  ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品搜索服务实现
 */
@Slf4j
@Service
public class ProductSearchServiceImpl implements ProductSearchService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private   ElasticsearchOperations   ElasticsearchOperations;
    
    @Autowired
    private ProductService productService;
    
    @Override
    public void syncProduct(Product product) {
        try {
            ProductDocument document = new ProductDocument();
            BeanUtils.copyProperties(product, document);
            productRepository.save(document);
            log.info("商品同步到Elasticsearch成功，productId={}", product.getId());
        } catch (Exception e) {
            log.error("商品同步到Elasticsearch失败", e);
        }
    }
    
    @Override
    public void deleteProduct(Long productId) {
        try {
            productRepository.deleteById(productId);
            log.info("从Elasticsearch删除商品成功，productId={}", productId);
        } catch (Exception e) {
            log.error("从Elasticsearch删除商品失败", e);
        }
    }
    
    @Override
    public List<Product> searchProducts(String keyword, Long tenantId) {
        // 如果没有传入 tenantId，从上下文获取
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
        }
        
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        
        try {
            // 构建查询条件
            Criteria criteria = new Criteria("tenantId").is(tenantId)
                    .and(new Criteria("status").is(1))
                    .and(new Criteria().or("name").contains(keyword)
                            .or("productCode").contains(keyword)
                            .or("description").contains(keyword));
            
            CriteriaQuery query = new CriteriaQuery(criteria);
            
            // 执行搜索
            SearchHits<ProductDocument> searchHits =   ElasticsearchOperations.search(query, ProductDocument.class);
            
            // 提取商品ID列表
            List<Long> productIds = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(ProductDocument::getId)
                    .collect(Collectors.toList());
            
            // 从数据库或缓存获取完整商品信息
            if (productIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            return productService.listByIds(productIds);
            
        } catch (Exception e) {
            log.error("Elasticsearch搜索商品失败，使用数据库查询", e);
            // 降级到数据库查询
            return productService.getProductList(keyword, null);
        }
    }
    
    @Override
    public List<Product> searchByCategory(String category) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        
        try {
            List<ProductDocument> documents = productRepository.findByTenantIdAndCategory(tenantId, category);
            
            List<Long> productIds = documents.stream()
                    .map(ProductDocument::getId)
                    .collect(Collectors.toList());
            
            if (productIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            return productService.listByIds(productIds);
            
        } catch (Exception e) {
            log.error("Elasticsearch按分类搜索商品失败，使用数据库查询", e);
            return productService.getProductList(null, category);
        }
    }
}
