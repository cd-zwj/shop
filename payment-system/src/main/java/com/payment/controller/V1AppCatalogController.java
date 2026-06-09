package com.payment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.Result;
import com.payment.entity.Product;
import com.payment.entity.Tenant;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.TenantMapper;
import com.payment.service.UserBehaviorLogService;
import com.payment.vo.ProductVO;
import com.payment.vo.TenantVO;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * v1 商户与商品浏览接口。
 */
@Slf4j
@RestController
@RequestMapping("/v1/app")
@RequiredArgsConstructor
public class V1AppCatalogController {

    private final TenantMapper tenantMapper;
    private final ProductMapper productMapper;
    private final UserBehaviorLogService userBehaviorLogService;

    @GetMapping("/tenants")
    public Result<List<TenantVO>> listTenants() {
        return Result.success(tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getStatus, 1)
                .eq(Tenant::getDeleted, 0)
                .orderByDesc(Tenant::getCreateTime))
                .stream().map(TenantVO::from).collect(Collectors.toList()));
    }

    @GetMapping("/tenants/{tenantId}")
    public Result<TenantVO> getTenant(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        return Result.success(TenantVO.from(tenant));
    }

    @GetMapping("/tenants/{tenantId}/products")
    public Result<List<ProductVO>> listProducts(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        return Result.success(productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getDeleted, 0)
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getCreateTime))
                .stream().map(ProductVO::from).collect(Collectors.toList()));
    }

    @GetMapping("/products/{productId}")
    public Result<ProductVO> getProduct(@PathVariable @Min(value = 1, message = "ID必须大于0") Long productId) {
        Product product = productMapper.selectById(productId);
        // 记录商品浏览行为（埋点失败不影响主流程）
        try {
            if (product != null) {
                userBehaviorLogService.recordBehavior(
                        null, product.getTenantId(), "VIEW",
                        "PRODUCT", productId, null);
            }
        } catch (Exception e) {
            log.warn("记录 VIEW 行为日志失败, productId={}", productId, e);
        }
        return Result.success(ProductVO.from(product));
    }
}
