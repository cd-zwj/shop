package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.AppCatalogProductSearchQueryDTO;
import com.payment.dto.AppCatalogSearchProductVO;
import com.payment.dto.AppCatalogSearchTenantVO;
import com.payment.dto.AppCatalogTenantSearchQueryDTO;

/**
 * 用户端公开搜索服务接口。
 *
 * <p>基于 Elasticsearch 提供面向 C 端用户的商品和商户全文搜索能力，
 * 承接 {@code V1AppCatalogSearchController} 的业务逻辑。</p>
 */
public interface AppCatalogSearchService {

    /**
     * 全文搜索商品，支持关键字、商户、分类等筛选条件。
     *
     * @param query 搜索查询条件（含关键字、分页、排序等）
     * @return 商品搜索结果分页
     */
    Page<AppCatalogSearchProductVO> searchProducts(AppCatalogProductSearchQueryDTO query);

    /**
     * 全文搜索商户，支持关键字模糊匹配商户名称等字段。
     *
     * @param query 搜索查询条件（含关键字、分页等）
     * @return 商户搜索结果分页
     */
    Page<AppCatalogSearchTenantVO> searchTenants(AppCatalogTenantSearchQueryDTO query);
}
