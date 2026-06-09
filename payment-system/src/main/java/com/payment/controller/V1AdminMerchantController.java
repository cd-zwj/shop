package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.MerchantDTO;
import com.payment.dto.MerchantDetailVO;
import com.payment.dto.MerchantListVO;
import com.payment.entity.Tenant;
import org.springframework.beans.BeanUtils;
import com.payment.service.V1AdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * v1 管理端商户接口。
 */
@RestController
@RequestMapping("/v1/admin/merchants")
@RequiredArgsConstructor
public class V1AdminMerchantController {

    private final V1AdminService v1AdminService;

    @SaCheckPermission("admin:merchant:list")
    @GetMapping
    public Result<PageResult<MerchantListVO>> listMerchants(@RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                             @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
                                                             @RequestParam(required = false) String name,
                                                             @RequestParam(required = false) Integer status) {
        Page<MerchantListVO> page = v1AdminService.listMerchants(current, size, name, status);
        return Result.success(PageResult.from(page));
    }

    @SaCheckPermission("admin:merchant:detail")
    @GetMapping("/{tenantId}")
    public Result<MerchantDetailVO> getMerchantDetail(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        return Result.success(v1AdminService.getMerchantDetail(tenantId));
    }

    @SaCheckPermission("admin:merchant:create")
    @PostMapping
    public Result<MerchantDetailVO> createMerchant(@Valid @RequestBody MerchantDTO dto) {
        Tenant tenant = v1AdminService.createMerchant(dto);
        MerchantDetailVO vo = new MerchantDetailVO();
        BeanUtils.copyProperties(tenant, vo);
        return Result.success(vo);
    }

    @SaCheckPermission("admin:merchant:update")
    @PutMapping("/{tenantId}")
    public Result<Void> updateMerchant(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @Valid @RequestBody MerchantDTO dto) {
        v1AdminService.updateMerchant(tenantId, dto);
        return Result.success();
    }

    @SaCheckPermission("admin:merchant:enable")
    @PutMapping("/{tenantId}/enable")
    public Result<Void> enableMerchant(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        v1AdminService.enableMerchant(tenantId);
        return Result.success();
    }

    @SaCheckPermission("admin:merchant:disable")
    @PutMapping("/{tenantId}/disable")
    public Result<Void> disableMerchant(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        v1AdminService.disableMerchant(tenantId);
        return Result.success();
    }
}
