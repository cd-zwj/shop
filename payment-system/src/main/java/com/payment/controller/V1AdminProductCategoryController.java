package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.common.Result;
import com.payment.entity.ProductCategory;
import com.payment.service.ProductCategoryService;
import com.payment.vo.ProductCategoryVO;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端平台级商品分类接口
 */
@RestController
@RequestMapping("/v1/admin/product-categories")
@RequiredArgsConstructor
public class V1AdminProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @SaCheckPermission(type = "admin", value = "admin:category:list")
    @GetMapping
    public Result<List<ProductCategoryVO>> listCategories() {
        return Result.success(productCategoryService.listTreeByTenant(null));
    }

    @SaCheckPermission(type = "admin", value = "admin:category:create")
    @PostMapping
    public Result<ProductCategoryVO> createCategory(@RequestBody AdminCategoryCreateDTO dto) {
        ProductCategory category = new ProductCategory();
        category.setTenantId(null);
        category.setName(dto.getName());
        category.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        category.setSortOrder(dto.getSortOrder());
        category.setIcon(dto.getIcon());
        category.setStatus(dto.getStatus());
        return Result.success(productCategoryService.create(category));
    }

    @SaCheckPermission(type = "admin", value = "admin:category:update")
    @PutMapping("/{id}")
    public Result<ProductCategoryVO> updateCategory(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long id,
            @RequestBody AdminCategoryUpdateDTO dto) {
        ProductCategory category = new ProductCategory();
        category.setName(dto.getName());
        category.setParentId(dto.getParentId());
        category.setSortOrder(dto.getSortOrder());
        category.setIcon(dto.getIcon());
        category.setStatus(dto.getStatus());
        return Result.success(productCategoryService.update(id, category));
    }

    @SaCheckPermission(type = "admin", value = "admin:category:delete")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable @Min(value = 1, message = "ID必须大于0") Long id) {
        productCategoryService.delete(id);
        return Result.success();
    }

    @Data
    public static class AdminCategoryCreateDTO {
        @NotBlank(message = "分类名称不能为空")
        private String name;
        private Long parentId;
        private Integer sortOrder;
        private String icon;
        private Integer status;
    }

    @Data
    public static class AdminCategoryUpdateDTO {
        private String name;
        private Long parentId;
        private Integer sortOrder;
        private String icon;
        private Integer status;
    }
}
