package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.constant.MerchantPermission;
import com.payment.dto.V1MerchantCardKeySummaryVO;
import com.payment.dto.V1MerchantCardKeyUploadDTO;
import com.payment.dto.V1MerchantCardKeyVO;
import com.payment.dto.V1MerchantProductChangeLogVO;
import com.payment.dto.V1MerchantProductUpsertDTO;
import com.payment.dto.V1MerchantProductVO;
import com.payment.service.CardKeyPoolService;
import com.payment.service.V1MerchantProductService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商户端商品管理控制器（Merchant 端）。
 * <p>提供商户对自有商品的 CRUD 操作，以及卡密池的查询、汇总和批量上传功能。
 * 需要商户角色登录，并通过商户员工本地权限矩阵控制访问。</p>
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/products")
@RequiredArgsConstructor
public class V1MerchantProductController {

    private final V1MerchantProductService v1MerchantProductService;
    private final CardKeyPoolService cardKeyPoolService;
    private final V1MerchantSupportService v1MerchantSupportService;

    @SaCheckLogin(type = "merchant")
    @GetMapping
    public Result<PageResult<V1MerchantProductVO>> listProducts(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                          @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                          @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
                                                          @RequestParam(required = false) String search,
                                                          @RequestParam(required = false) String category,
                                                          @RequestParam(required = false) String status) {
        Long platformUserId = requireProductPermission(tenantId);
        Page<V1MerchantProductVO> page = v1MerchantProductService.listProducts(
                tenantId, platformUserId, current, size, search, category, status);
        return Result.success(new PageResult<>(page.getRecords(), page.getTotal(), (int) page.getCurrent(), (int) page.getSize()));
    }

    @SaCheckLogin(type = "merchant")
    @GetMapping("/{productId}")
    public Result<V1MerchantProductVO> getProduct(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId) {
        Long platformUserId = requireProductPermission(tenantId);
        return Result.success(v1MerchantProductService.getProduct(tenantId, platformUserId, productId));
    }

    @SaCheckLogin(type = "merchant")
    @GetMapping("/{productId}/change-logs")
    public Result<PageResult<V1MerchantProductChangeLogVO>> listChangeLogs(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size) {
        Long platformUserId = requireProductPermission(tenantId);
        Page<V1MerchantProductChangeLogVO> page = v1MerchantProductService.listProductChangeLogs(
                tenantId, platformUserId, productId, current, size);
        return Result.success(PageResult.from(page));
    }

    @SaCheckLogin(type = "merchant")
    @PostMapping
    public Result<V1MerchantProductVO> createProduct(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                     @Valid @RequestBody V1MerchantProductUpsertDTO dto) {
        Long platformUserId = requireProductPermission(tenantId);
        return Result.success(v1MerchantProductService.createProduct(tenantId, platformUserId, dto));
    }

    @SaCheckLogin(type = "merchant")
    @PutMapping("/{productId}")
    public Result<V1MerchantProductVO> updateProduct(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                     @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId,
                                                     @Valid @RequestBody V1MerchantProductUpsertDTO dto) {
        Long platformUserId = requireProductPermission(tenantId);
        return Result.success(v1MerchantProductService.updateProduct(tenantId, platformUserId, productId, dto));
    }

    @SaCheckLogin(type = "merchant")
    @DeleteMapping("/{productId}")
    public Result<Void> deleteProduct(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId) {
        Long platformUserId = requireProductPermission(tenantId);
        v1MerchantProductService.deleteProduct(tenantId, platformUserId, productId);
        return Result.success();
    }

    @SaCheckLogin(type = "merchant")
    @GetMapping("/{productId}/card-keys")
    public Result<PageResult<V1MerchantCardKeyVO>> listCardKeys(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                                @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId,
                                                                @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                                @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
                                                                @RequestParam(required = false) String status) {
        Long platformUserId = requireProductPermission(tenantId);
        Page<V1MerchantCardKeyVO> page = cardKeyPoolService.listMerchantCardKeys(
                tenantId, platformUserId, productId, current, size, status);
        return Result.success(new PageResult<>(page.getRecords(), page.getTotal(), (int) page.getCurrent(), (int) page.getSize()));
    }

    @SaCheckLogin(type = "merchant")
    @GetMapping("/{productId}/card-keys/summary")
    public Result<V1MerchantCardKeySummaryVO> getCardKeySummary(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                               @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId) {
        Long platformUserId = requireProductPermission(tenantId);
        return Result.success(cardKeyPoolService.getMerchantSummary(tenantId, platformUserId, productId));
    }

    @SaCheckLogin(type = "merchant")
    @PostMapping("/{productId}/card-keys/upload")
    public Result<V1MerchantCardKeySummaryVO> uploadCardKeys(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                            @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId,
                                                            @Valid @RequestBody V1MerchantCardKeyUploadDTO dto) {
        Long platformUserId = requireProductPermission(tenantId);
        return Result.success(cardKeyPoolService.uploadMerchantCardKeys(tenantId, platformUserId, productId, dto));
    }

    private Long requireProductPermission(Long tenantId) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);
        return platformUserId;
    }
}
