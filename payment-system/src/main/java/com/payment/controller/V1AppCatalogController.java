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
 * v1 商户与商品浏览接口。
 */
@RestController
@RequestMapping("/v1/app")
@RequiredArgsConstructor
public class V1AppCatalogController {

    private final AppCatalogService appCatalogService;

    @GetMapping("/tenants")
    public Result<List<TenantVO>> listTenants() {
        return Result.success(appCatalogService.listActiveTenants()
                .stream().map(TenantVO::from).collect(Collectors.toList()));
    }

    @GetMapping("/tenants/{tenantId}")
    public Result<TenantVO> getTenant(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        return Result.success(TenantVO.from(appCatalogService.getTenant(tenantId)));
    }

    @GetMapping("/tenants/{tenantId}/products")
    public Result<List<ProductVO>> listProducts(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        return Result.success(appCatalogService.listTenantProducts(tenantId)
                .stream().map(ProductVO::from).collect(Collectors.toList()));
    }

    @GetMapping("/tenants/{tenantId}/products/search")
    public Result<List<ProductVO>> searchProducts(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                  @RequestParam String keyword) {
        return Result.success(appCatalogService.searchTenantProducts(keyword, tenantId)
                .stream().map(ProductVO::from).collect(Collectors.toList()));
    }

    @GetMapping("/products/{productId}")
    public Result<ProductVO> getProduct(@PathVariable @Min(value = 1, message = "ID必须大于0") Long productId) {
        return Result.success(ProductVO.from(appCatalogService.getProductAndRecordView(productId)));
    }
}
