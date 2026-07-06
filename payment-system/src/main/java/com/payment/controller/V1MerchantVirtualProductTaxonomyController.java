package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.Result;
import com.payment.constant.MerchantPermission;
import com.payment.dto.VirtualProductCategoryUpsertDTO;
import com.payment.dto.VirtualProductCategoryVO;
import com.payment.dto.VirtualProductTypeUpsertDTO;
import com.payment.dto.VirtualProductTypeVO;
import com.payment.service.VirtualProductTaxonomyService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}")
@RequiredArgsConstructor
public class V1MerchantVirtualProductTaxonomyController {

    private final VirtualProductTaxonomyService virtualProductTaxonomyService;
    private final V1MerchantSupportService v1MerchantSupportService;

    @SaCheckLogin(type = "merchant")
    @GetMapping("/virtual-product-types")
    public Result<List<VirtualProductTypeVO>> listTypes(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @RequestParam(required = false) Integer status) {
        Long platformUserId = requireProductPermission(tenantId);
        return Result.success(virtualProductTaxonomyService.listTypes(tenantId, platformUserId, status));
    }

    @SaCheckLogin(type = "merchant")
    @PostMapping("/virtual-product-types")
    public Result<VirtualProductTypeVO> createType(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @Valid @RequestBody VirtualProductTypeUpsertDTO dto) {
        Long platformUserId = requireProductPermission(tenantId);
        return Result.success(virtualProductTaxonomyService.createType(tenantId, platformUserId, dto));
    }

    @SaCheckLogin(type = "merchant")
    @PutMapping("/virtual-product-types/{id}")
    public Result<VirtualProductTypeVO> updateType(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long id,
            @Valid @RequestBody VirtualProductTypeUpsertDTO dto) {
        Long platformUserId = requireProductPermission(tenantId);
        return Result.success(virtualProductTaxonomyService.updateType(tenantId, platformUserId, id, dto));
    }

    @SaCheckLogin(type = "merchant")
    @DeleteMapping("/virtual-product-types/{id}")
    public Result<Void> deleteType(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long id) {
        Long platformUserId = requireProductPermission(tenantId);
        virtualProductTaxonomyService.deleteType(tenantId, platformUserId, id);
        return Result.success();
    }

    @SaCheckLogin(type = "merchant")
    @GetMapping("/virtual-product-categories")
    public Result<List<VirtualProductCategoryVO>> listCategories(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) Integer status) {
        Long platformUserId = requireProductPermission(tenantId);
        return Result.success(virtualProductTaxonomyService.listCategories(tenantId, platformUserId, typeId, status));
    }

    @SaCheckLogin(type = "merchant")
    @PostMapping("/virtual-product-categories")
    public Result<VirtualProductCategoryVO> createCategory(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @Valid @RequestBody VirtualProductCategoryUpsertDTO dto) {
        Long platformUserId = requireProductPermission(tenantId);
        return Result.success(virtualProductTaxonomyService.createCategory(tenantId, platformUserId, dto));
    }

    @SaCheckLogin(type = "merchant")
    @PutMapping("/virtual-product-categories/{id}")
    public Result<VirtualProductCategoryVO> updateCategory(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long id,
            @Valid @RequestBody VirtualProductCategoryUpsertDTO dto) {
        Long platformUserId = requireProductPermission(tenantId);
        return Result.success(virtualProductTaxonomyService.updateCategory(tenantId, platformUserId, id, dto));
    }

    @SaCheckLogin(type = "merchant")
    @DeleteMapping("/virtual-product-categories/{id}")
    public Result<Void> deleteCategory(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long id) {
        Long platformUserId = requireProductPermission(tenantId);
        virtualProductTaxonomyService.deleteCategory(tenantId, platformUserId, id);
        return Result.success();
    }

    private Long requireProductPermission(Long tenantId) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);
        return platformUserId;
    }
}
