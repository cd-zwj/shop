package com.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.Result;
import com.payment.dto.MerchantDTO;
import com.payment.dto.MerchantDetailVO;
import com.payment.dto.MerchantQueryDTO;
import com.payment.entity.Tenant;
import com.payment.service.MerchantService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 商家管理控制器
 * @deprecated 功能已被 V1 版本完全替代
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant")
@Deprecated
@Profile({"dev", "test"})
public class MerchantController {
    
    @Autowired
    private MerchantService merchantService;

    /**
     * 创建商家（管理端）
     */
    @SaCheckLogin
    @PostMapping("/admin/create")

    public Result<Map<String, Object>> createMerchant(@Validated @RequestBody MerchantDTO dto) {
        log.info("创建商家，tenantCode: {}, name: {}", dto.getTenantCode(), dto.getName());
        
        Tenant tenant = merchantService.createMerchant(dto);
        
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenant.getId());
        result.put("tenantCode", tenant.getTenantCode());
        result.put("message", "商家创建成功");
        
        return Result.success(result);
    }
    
    /**
     * 更新商家信息（管理端）
     */
    @SaCheckLogin
    @PutMapping("/admin/update/{tenantId}")

    public Result<Void> updateMerchant(
             @PathVariable Long tenantId,
            @Validated @RequestBody MerchantDTO dto) {
        log.info("更新商家信息，tenantId: {}", tenantId);
        
        merchantService.updateMerchant(tenantId, dto);
        
        return Result.success();
    }
    
    /**
     * 启用商家（管理端）
     */
    @SaCheckLogin
    @PostMapping("/admin/enable/{tenantId}")

    public Result<Void> enableMerchant( @PathVariable Long tenantId) {
        log.info("启用商家，tenantId: {}", tenantId);
        
        merchantService.enableMerchant(tenantId);
        
        return Result.success();
    }
    
    /**
     * 禁用商家（管理端）
     */
    @SaCheckLogin
    @PostMapping("/admin/disable/{tenantId}")

    public Result<Void> disableMerchant(@PathVariable Long tenantId) {
        log.info("禁用商家，tenantId: {}", tenantId);
        
        merchantService.disableMerchant(tenantId);
        
        return Result.success();
    }
    
    /**
     * 商家列表（管理端）
     */
    @SaCheckLogin
    @GetMapping("/admin/list")

    public Result<Page<Tenant>> listMerchants(
           @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
           @RequestParam(defaultValue = "1") Integer pageNum,
          @RequestParam(defaultValue = "10") Integer pageSize) {
        
        MerchantQueryDTO query = new MerchantQueryDTO();
        query.setName(name);
        query.setStatus(status);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        
        Page<Tenant> page = merchantService.listMerchants(query);
        
        return Result.success(page);
    }
    
    /**
     * 商家详情
     */
    @SaCheckLogin
    @GetMapping("/detail/{tenantId}")

    public Result<MerchantDetailVO> getMerchantDetail( @PathVariable Long tenantId) {
        log.info("查询商家详情，tenantId: {}", tenantId);
        
        MerchantDetailVO detail = merchantService.getMerchantDetail(tenantId);
        
        return Result.success(detail);
    }
}
