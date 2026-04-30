package com.payment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.Result;
import com.payment.entity.Product;
import com.payment.entity.Tenant;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * v1 商户与商品浏览接口。
 */
@RestController
@RequestMapping("/v1/app")
@RequiredArgsConstructor
public class V1AppCatalogController {

    private final TenantMapper tenantMapper;
    private final ProductMapper productMapper;

    @GetMapping("/tenants")
    public Result<List<Tenant>> listTenants() {
        return Result.success(tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getStatus, 1)
                .eq(Tenant::getDeleted, 0)
                .orderByDesc(Tenant::getCreateTime)));
    }

    @GetMapping("/tenants/{tenantId}")
    public Result<Tenant> getTenant(@PathVariable Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        return Result.success(tenant);
    }

    @GetMapping("/tenants/{tenantId}/products")
    public Result<List<Product>> listProducts(@PathVariable Long tenantId) {
        return Result.success(productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getDeleted, 0)
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getCreateTime)));
    }

    @GetMapping("/products/{productId}")
    public Result<Product> getProduct(@PathVariable Long productId) {
        return Result.success(productMapper.selectById(productId));
    }
}
