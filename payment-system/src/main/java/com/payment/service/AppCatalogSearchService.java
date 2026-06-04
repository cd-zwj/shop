package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.AppCatalogProductSearchQueryDTO;
import com.payment.dto.AppCatalogSearchProductVO;
import com.payment.dto.AppCatalogSearchTenantVO;
import com.payment.dto.AppCatalogTenantSearchQueryDTO;

/**
 * 用户端公开搜索服务。
 */
public interface AppCatalogSearchService {

    Page<AppCatalogSearchProductVO> searchProducts(AppCatalogProductSearchQueryDTO query);

    Page<AppCatalogSearchTenantVO> searchTenants(AppCatalogTenantSearchQueryDTO query);
}
