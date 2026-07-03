package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.BusinessException;
import com.payment.common.Result;
import com.payment.entity.ProductCategory;
import com.payment.service.ProductCategoryService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.ProductCategoryVO;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商户端商品分类管理控制器（Merchant 端）。
 * <p>提供商户对商品分类的树形查询、新增、修改和删除操作。
 * 所有操作均需验证当前用户是否为该租户的有效员工。</p>
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/product-categories")
@RequiredArgsConstructor
@SaCheckLogin(type = "merchant")
public class V1MerchantProductCategoryController {

    private final ProductCategoryService productCategoryService;
    private final V1MerchantSupportService v1MerchantSupportService;

    /**
     * 查询当前租户的商品分类树。
     *
     * @param tenantId 租户 ID
     * @return 分类树形结构列表
     */
    @GetMapping
    public Result<List<ProductCategoryVO>> listCategories(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        return Result.success(productCategoryService.listTreeByTenant(tenantId));
    }

    /**
     * 创建商品分类。
     *
     * @param tenantId 租户 ID
     * @param dto      分类创建参数（名称、父级、排序、图标、状态）
     * @return 创建后的分类信息
     */
    @PostMapping
    public Result<ProductCategoryVO> createCategory(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @RequestBody ProductCategoryCreateDTO dto) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        ProductCategory category = new ProductCategory();
        category.setTenantId(tenantId);
        category.setName(dto.getName());
        category.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        category.setSortOrder(dto.getSortOrder());
        category.setIcon(dto.getIcon());
        category.setStatus(dto.getStatus());
        return Result.success(productCategoryService.create(category));
    }

    /**
     * 更新商品分类。
     *
     * @param tenantId 租户 ID
     * @param id       分类 ID
     * @param dto      分类更新参数
     * @return 更新后的分类信息
     */
    @PutMapping("/{id}")
    public Result<ProductCategoryVO> updateCategory(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long id,
            @RequestBody ProductCategoryUpdateDTO dto) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        verifyBelongsToTenant(id, tenantId);
        ProductCategory category = new ProductCategory();
        category.setName(dto.getName());
        category.setParentId(dto.getParentId());
        category.setSortOrder(dto.getSortOrder());
        category.setIcon(dto.getIcon());
        category.setStatus(dto.getStatus());
        return Result.success(productCategoryService.update(id, category));
    }

    /**
     * 删除商品分类。
     *
     * @param tenantId 租户 ID
     * @param id       分类 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long id) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        verifyBelongsToTenant(id, tenantId);
        productCategoryService.delete(id);
        return Result.success();
    }

    /**
     * 验证分类是否属于当前租户。
     *
     * @param categoryId 分类 ID
     * @param tenantId   租户 ID
     * @throws BusinessException 分类不存在或不属于当前租户时抛出异常
     */
    private void verifyBelongsToTenant(Long categoryId, Long tenantId) {
        ProductCategory existing = productCategoryService.getById(categoryId);
        if (existing == null || existing.getDeleted() == 1) {
            throw new BusinessException("分类不存在");
        }
        if (existing.getTenantId() == null || !existing.getTenantId().equals(tenantId)) {
            throw new BusinessException("无权操作该分类");
        }
    }

    @Data
    public static class ProductCategoryCreateDTO {
        @NotBlank(message = "分类名称不能为空")
        private String name;
        private Long parentId;
        private Integer sortOrder;
        private String icon;
        private Integer status;
    }

    @Data
    public static class ProductCategoryUpdateDTO {
        private String name;
        private Long parentId;
        private Integer sortOrder;
        private String icon;
        private Integer status;
    }
}
