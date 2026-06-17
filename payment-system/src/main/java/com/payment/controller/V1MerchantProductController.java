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

@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/products")
@RequiredArgsConstructor
public class V1MerchantProductController {

    private final V1MerchantProductService v1MerchantProductService;
    private final CardKeyPoolService cardKeyPoolService;

    @SaCheckPermission("merchant:product:read")
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

    @SaCheckPermission("merchant:product:read")
    @GetMapping("/{productId}")
    public Result<V1MerchantProductVO> getProduct(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId) {
        return Result.success(v1MerchantProductService.getProduct(tenantId, PlatformSessionHelper.getPlatformUserId(), productId));
    }

    @SaCheckPermission("merchant:product:write")
    @PostMapping
    public Result<V1MerchantProductVO> createProduct(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                     @Valid @RequestBody V1MerchantProductUpsertDTO dto) {
        return Result.success(v1MerchantProductService.createProduct(tenantId, PlatformSessionHelper.getPlatformUserId(), dto));
    }

    @SaCheckPermission("merchant:product:write")
    @PutMapping("/{productId}")
    public Result<V1MerchantProductVO> updateProduct(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                     @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId,
                                                     @Valid @RequestBody V1MerchantProductUpsertDTO dto) {
        return Result.success(v1MerchantProductService.updateProduct(tenantId, PlatformSessionHelper.getPlatformUserId(), productId, dto));
    }

    @SaCheckPermission("merchant:product:write")
    @DeleteMapping("/{productId}")
    public Result<Void> deleteProduct(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId) {
        v1MerchantProductService.deleteProduct(tenantId, PlatformSessionHelper.getPlatformUserId(), productId);
        return Result.success();
    }

    @SaCheckPermission("merchant:product:read")
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

    @SaCheckPermission("merchant:product:read")
    @GetMapping("/{productId}/card-keys/summary")
    public Result<V1MerchantCardKeySummaryVO> getCardKeySummary(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                               @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId) {
        return Result.success(cardKeyPoolService.getMerchantSummary(
                tenantId, PlatformSessionHelper.getPlatformUserId(), productId));
    }

    @SaCheckPermission("merchant:product:write")
    @PostMapping("/{productId}/card-keys/upload")
    public Result<V1MerchantCardKeySummaryVO> uploadCardKeys(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                            @PathVariable @Min(value = 1, message = "ID必须大于0") Long productId,
                                                            @Valid @RequestBody V1MerchantCardKeyUploadDTO dto) {
        return Result.success(cardKeyPoolService.uploadMerchantCardKeys(
                tenantId, PlatformSessionHelper.getPlatformUserId(), productId, dto));
    }
}
