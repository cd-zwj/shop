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
 * 商品搜索服务实现类。
 * <p>
 * 基于 Elasticsearch 提供全文搜索能力，在 ES 不可用或未启用时自动降级到数据库查询，
 * 保证商品搜索功能的高可用性。所有查询均受多租户隔离约束（tenant_id 过滤）。
 * </p>
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
    
    /**
     * 将单个商品同步到 Elasticsearch 索引。
     * <p>
     * ES 未启用时静默跳过；同步失败仅记录日志，不抛出异常，避免影响主流程。
     * </p>
     *
     * @param product 待同步的商品实体
     */
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
    
    /**
     * 从 Elasticsearch 索引中删除指定商品。
     * <p>
     * ES 未启用时静默跳过；删除失败仅记录日志，不抛出异常。
     * </p>
     *
     * @param productId 待删除的商品 ID
     */
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
    
    /**
     * 按关键字全文搜索商品（商品名称、编码、描述）。
     * <p>
     * 优先使用 Elasticsearch 进行全文检索，ES 不可用时自动降级为数据库模糊查询。
     * 仅返回当前租户下已上架（status=1）的商品。
     * </p>
     *
     * @param keyword  搜索关键字
     * @param tenantId 租户 ID，为 null 时从 {@link TenantContextHolder} 获取
     * @return 匹配的商品列表，无结果时返回空列表
     * @throws BusinessException 租户信息不存在时抛出
     */
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

    /**
     * 按商品分类搜索商品。
     * <p>
     * 优先使用 Elasticsearch 进行分类精确匹配，ES 不可用时自动降级为数据库查询。
     * 查询范围限定为当前租户下未删除的所有商品（不过滤上架状态）。
     * </p>
     *
     * @param category 商品分类名称
     * @return 该分类下的商品列表，无结果时返回空列表
     * @throws BusinessException 租户信息不存在时抛出
     */
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

    /**
     * 将 Elasticsearch 文档对象转换为商品实体。
     *
     * @param document ES 商品文档
     * @return 商品实体，deleted 标记重置为 0
     */
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
