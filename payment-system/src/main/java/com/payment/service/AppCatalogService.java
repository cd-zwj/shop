package com.payment.service;

import com.payment.entity.Product;
import com.payment.entity.Tenant;

import java.util.List;

/**
 * 用户端商户与商品浏览服务。
 *
 * <p>承接 {@code V1AppCatalogController} 的数据访问逻辑，Controller 仅负责 VO 转换。</p>
 */
public interface AppCatalogService {

    /**
     * 查询启用中的商户列表（按创建时间倒序）。
     */
    List<Tenant> listActiveTenants();

    /**
     * 查询商户详情，不存在时返回 {@code null}。
     */
    Tenant getTenant(Long tenantId);

    /**
     * 查询指定商户的上架商品列表（按创建时间倒序）。
     */
    List<Product> listTenantProducts(Long tenantId);

    /**
     * 按关键字搜索指定商户的上架商品。
     */
    List<Product> searchTenantProducts(String keyword, Long tenantId);

    /**
     * 查询商品详情并记录浏览行为；商品不存在时返回 {@code null}，埋点失败不影响主流程。
     */
    Product getProductAndRecordView(Long productId);
}
