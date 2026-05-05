package com.payment.repository;
import com.payment.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
/**
 * 商品Elasticsearch Repository
 */
@Repository
public interface ProductRepository extends ElasticsearchRepository<ProductDocument, Long> {
    /**
     * 根据租户ID和商品名称搜索
     */
    List<ProductDocument> findByTenantIdAndNameContaining(Long tenantId, String name);
    /**
     * 根据租户ID和商品编码搜索
     */
    List<ProductDocument> findByTenantIdAndProductCodeContaining(Long tenantId, String productCode);
    /**
     * 根据租户ID和分类搜索
     */
    List<ProductDocument> findByTenantIdAndCategory(Long tenantId, String category);
    /**
     * 根据租户ID和状态搜索
     */
    List<ProductDocument> findByTenantIdAndStatus(Long tenantId, Integer status);
}
