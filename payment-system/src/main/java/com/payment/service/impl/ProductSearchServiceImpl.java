package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.document.ProductDocument;
import com.payment.entity.Product;
import com.payment.mapper.ProductMapper;
import com.payment.repository.ProductRepository;
import com.payment.service.ProductSearchService;
import com.payment.util.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 商品搜索服务实现
 */
@Slf4j
@Service
public class ProductSearchServiceImpl implements ProductSearchService {
    
    @Autowired(required = false)
    private ProductRepository productRepository;
    
    @Autowired(required = false)
    private ElasticsearchOperations elasticsearchOperations;
    
    @Autowired
    private ProductMapper productMapper;
    
    @Override
    public void syncProduct(Product product) {
        try {
            if (productRepository == null) {
                log.warn("商品索引同步已跳过，Elasticsearch repository 未启用，productId={}", product.getId());
                return;
            }
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
            if (productRepository == null) {
                log.warn("商品索引删除已跳过，Elasticsearch repository 未启用，productId={}", productId);
                return;
            }
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

        // ES 未启用时直接走 DB，避免 ERROR 日志噪音
        if (elasticsearchOperations == null) {
            log.debug("Elasticsearch 未启用，使用数据库查询商品搜索，tenantId={}, keyword={}", tenantId, keyword);
            return queryProducts(tenantId, keyword, null, true);
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
            SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(query, ProductDocument.class);

            return searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(this::toProduct)
                    .toList();

        } catch (Exception e) {
            log.error("Elasticsearch搜索商品失败，使用数据库查询", e);
            return queryProducts(tenantId, keyword, null, true);
        }
    }

    @Override
    public List<Product> searchByCategory(String category) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }

        // ES 未启用时直接走 DB
        if (elasticsearchOperations == null) {
            log.debug("Elasticsearch 未启用，使用数据库查询商品分类，tenantId={}, category={}", tenantId, category);
            return queryProducts(tenantId, null, category, false);
        }

        try {
            Criteria criteria = new Criteria("tenantId").is(tenantId)
                    .and(new Criteria("category").is(category));

            SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(new CriteriaQuery(criteria), ProductDocument.class);
            return searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(this::toProduct)
                    .toList();

        } catch (Exception e) {
            log.error("Elasticsearch按分类搜索商品失败，使用数据库查询", e);
            return queryProducts(tenantId, null, category, false);
        }
    }

    private Product toProduct(ProductDocument document) {
        Product product = new Product();
        BeanUtils.copyProperties(document, product);
        product.setDeleted(0);
        return product;
    }

    /**
     * 数据库降级查询。
     *
     * @param tenantId      租户 ID
     * @param keyword       关键字（按 name / productCode 模糊匹配）
     * @param category      分类（精确匹配）
     * @param onlyActive    true 时仅返回上架商品（status=1），与 ES 全文搜索路径保持一致；
     *                      false 时不过滤 status，与 ES 分类查询路径保持一致
     */
    private List<Product> queryProducts(Long tenantId, String keyword, String category, boolean onlyActive) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getDeleted, 0)
                .eq(onlyActive, Product::getStatus, 1)
                .orderByDesc(Product::getCreateTime);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(query -> query.like(Product::getName, keyword)
                    .or()
                    .like(Product::getProductCode, keyword));
        }

        if (category != null && !category.isBlank()) {
            wrapper.eq(Product::getCategory, category);
        }

        List<Product> products = productMapper.selectList(wrapper);
        return products == null ? new ArrayList<>() : products;
    }
}
