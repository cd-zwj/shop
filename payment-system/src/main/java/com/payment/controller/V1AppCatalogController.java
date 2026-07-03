package com.payment.controller;

import com.payment.common.Result;
import com.payment.service.AppCatalogService;
import com.payment.vo.ProductVO;
import com.payment.vo.TenantVO;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * C端商品目录浏览控制器。
 * <p>
 * 提供商户列表浏览、商户详情查看、商品列表和商品搜索等接口，
 * 用于C端用户在首页或商户页面浏览可购买的商品。
 * <p>
 * 路径前缀：/v1/app，无需登录即可访问。
 *
 * @author payment-system
 */
@RestController
@RequestMapping("/v1/app")
@RequiredArgsConstructor
public class V1AppCatalogController {

    private final AppCatalogService appCatalogService;

    /**
     * 获取已入驻商户列表。
     * <p>
     * 返回所有状态为营业中的商户信息，供C端首页展示。
     *
     * @return 商户信息列表
     */
    @GetMapping("/tenants")
    public Result<List<TenantVO>> listTenants() {
        return Result.success(appCatalogService.listActiveTenants()
                .stream().map(TenantVO::from).collect(Collectors.toList()));
    }

    /**
     * 获取商户详情。
     * <p>
     * 根据商户ID获取商户的详细信息，包括名称、logo、简介等。
     *
     * @param tenantId 商户ID，必须大于0
     * @return 商户详情信息
     */
    @GetMapping("/tenants/{tenantId}")
    public Result<TenantVO> getTenant(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        return Result.success(TenantVO.from(appCatalogService.getTenant(tenantId)));
    }

    /**
     * 获取商户下的商品列表。
     * <p>
     * 根据商户ID查询该商户下所有上架商品，按排序权重展示。
     *
     * @param tenantId 商户ID，必须大于0
     * @return 商品列表
     */
    @GetMapping("/tenants/{tenantId}/products")
    public Result<List<ProductVO>> listProducts(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        return Result.success(appCatalogService.listTenantProducts(tenantId)
                .stream().map(ProductVO::from).collect(Collectors.toList()));
    }

    /**
     * 搜索商户下的商品。
     * <p>
     * 根据关键字在指定商户的商品中进行搜索，支持Elasticsearch全文检索。
     *
     * @param tenantId 商户ID，必须大于0
     * @param keyword  搜索关键字
     * @return 匹配的商品列表
     */
    @GetMapping("/tenants/{tenantId}/products/search")
    public Result<List<ProductVO>> searchProducts(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                  @RequestParam String keyword) {
        return Result.success(appCatalogService.searchTenantProducts(keyword, tenantId)
                .stream().map(ProductVO::from).collect(Collectors.toList()));
    }

    /**
     * 获取商品详情。
     * <p>
     * 根据商品ID获取商品的完整信息，包括名称、价格、描述、库存、图片等。
     * 同时会记录用户的商品浏览行为。
     *
     * @param productId 商品ID，必须大于0
     * @return 商品详情信息
     */
    @GetMapping("/products/{productId}")
    public Result<ProductVO> getProduct(@PathVariable @Min(value = 1, message = "ID必须大于0") Long productId) {
        return Result.success(ProductVO.from(appCatalogService.getProductAndRecordView(productId)));
    }
}
