package com.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.V1MerchantCardKeySummaryVO;
import com.payment.dto.V1MerchantCardKeyUploadDTO;
import com.payment.dto.V1MerchantCardKeyVO;
import com.payment.dto.V1MerchantProductUpsertDTO;
import com.payment.dto.V1MerchantProductVO;
import com.payment.service.CardKeyPoolService;
import com.payment.service.V1MerchantProductService;
import com.payment.util.PlatformSessionHelper;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商户端商品管理控制器（Merchant 端）。
 * <p>提供商户对自有商品的 CRUD 操作，以及卡密池的查询、汇总和批量上传功能。
 * 需要商户角色登录，并通过 RBAC 权限（merchant:product:read / write）控制访问。</p>
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/products")
@RequiredArgsConstructor
public class V1MerchantProductController {

    private final V1MerchantProductService v1MerchantProductService;
    private final CardKeyPoolService cardKeyPoolService;

    /**
     * 分页查询商户商品列表。
     *
     * @param tenantId 租户 ID
     * @param current  当前页码，默认 1
     * @param size     每页条数，默认 10
     * @param search   搜索关键字（可选）
     * @param category 商品分类筛选（可选）
     * @param status   商品状态筛选（可选）
     * @return 商品分页列表
     */
    @SaCheckPermission(type = "merchant", value = "merchant:product:read")
    @GetMapping
    public Result<PageResult<V1MerchantProductVO>> listProducts(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                          @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                          @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
                                                          @RequestParam(required = false) String search,
                                                          @RequestParam(required = false) String category,
                                                          @RequestParam(required = false) String status) {
        Page<V1MerchantProductVO> page = v1MerchantProductService.listProducts(
                tenantId, PlatformSessionHelper.getPlatformUserId(), current, size, search, category, status);
        return Result.success(new PageResult<>(page.getRecords(), page.getTotal(), (int) page.getCurrent(), (int) page.getSize()));
    }

    /**
     * 获取商品详情。
     *
     * @param tenantId  租户 ID
     * @param productId 商品 ID
     * @return 商品详情信息
     */
    @SaCheckPermission(type = "merchant", value = "merchant:product:read")
    @GetMapping("/{productId}")
    public Result<V1MerchantProductVO> getProduct(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId) {
        return Result.success(v1MerchantProductService.getProduct(tenantId, PlatformSessionHelper.getPlatformUserId(), productId));
    }

    /**
     * 创建商品。
     *
     * @param tenantId 租户 ID
     * @param dto      商品创建参数
     * @return 创建后的商品信息
     */
    @SaCheckPermission(type = "merchant", value = "merchant:product:write")
    @PostMapping
    public Result<V1MerchantProductVO> createProduct(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                     @Valid @RequestBody V1MerchantProductUpsertDTO dto) {
        return Result.success(v1MerchantProductService.createProduct(tenantId, PlatformSessionHelper.getPlatformUserId(), dto));
    }

    /**
     * 更新商品信息。
     *
     * @param tenantId  租户 ID
     * @param productId 商品 ID
     * @param dto       商品更新参数
     * @return 更新后的商品信息
     */
    @SaCheckPermission(type = "merchant", value = "merchant:product:write")
    @PutMapping("/{productId}")
    public Result<V1MerchantProductVO> updateProduct(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                     @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId,
                                                     @Valid @RequestBody V1MerchantProductUpsertDTO dto) {
        return Result.success(v1MerchantProductService.updateProduct(tenantId, PlatformSessionHelper.getPlatformUserId(), productId, dto));
    }

    /**
     * 删除商品。
     *
     * @param tenantId  租户 ID
     * @param productId 商品 ID
     * @return 操作结果
     */
    @SaCheckPermission(type = "merchant", value = "merchant:product:write")
    @DeleteMapping("/{productId}")
    public Result<Void> deleteProduct(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId) {
        v1MerchantProductService.deleteProduct(tenantId, PlatformSessionHelper.getPlatformUserId(), productId);
        return Result.success();
    }

    /**
     * 分页查询商品的卡密列表。
     *
     * @param tenantId  租户 ID
     * @param productId 商品 ID
     * @param current   当前页码，默认 1
     * @param size      每页条数，默认 10
     * @param status    卡密状态筛选（可选）
     * @return 卡密分页列表
     */
    @SaCheckPermission(type = "merchant", value = "merchant:product:read")
    @GetMapping("/{productId}/card-keys")
    public Result<PageResult<V1MerchantCardKeyVO>> listCardKeys(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                                @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId,
                                                                @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                                @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
                                                                @RequestParam(required = false) String status) {
        Page<V1MerchantCardKeyVO> page = cardKeyPoolService.listMerchantCardKeys(
                tenantId, PlatformSessionHelper.getPlatformUserId(), productId, current, size, status);
        return Result.success(new PageResult<>(page.getRecords(), page.getTotal(), (int) page.getCurrent(), (int) page.getSize()));
    }

    /**
     * 获取商品卡密池汇总信息。
     *
     * @param tenantId  租户 ID
     * @param productId 商品 ID
     * @return 卡密池汇总（总数、已售、可用等统计）
     */
    @SaCheckPermission(type = "merchant", value = "merchant:product:read")
    @GetMapping("/{productId}/card-keys/summary")
    public Result<V1MerchantCardKeySummaryVO> getCardKeySummary(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                               @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId) {
        return Result.success(cardKeyPoolService.getMerchantSummary(
                tenantId, PlatformSessionHelper.getPlatformUserId(), productId));
    }

    /**
     * 批量上传卡密到商品的卡密池。
     *
     * @param tenantId  租户 ID
     * @param productId 商品 ID
     * @param dto       卡密上传数据
     * @return 上传后的卡密池汇总信息
     */
    @SaCheckPermission(type = "merchant", value = "merchant:product:write")
    @PostMapping("/{productId}/card-keys/upload")
    public Result<V1MerchantCardKeySummaryVO> uploadCardKeys(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                            @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId,
                                                            @Valid @RequestBody V1MerchantCardKeyUploadDTO dto) {
        return Result.success(cardKeyPoolService.uploadMerchantCardKeys(
                tenantId, PlatformSessionHelper.getPlatformUserId(), productId, dto));
    }
}
