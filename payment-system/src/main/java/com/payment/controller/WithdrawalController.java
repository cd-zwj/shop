package com.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.annotation.RequireAuth;
import com.payment.common.Result;
import com.payment.dto.WithdrawalApplyDTO;
import com.payment.dto.WithdrawalApproveDTO;
import com.payment.dto.WithdrawalQueryDTO;
import com.payment.entity.MerchantBalance;
import com.payment.entity.Withdrawal;
import com.payment.service.WithdrawalService;
import com.payment.util.TenantContextHolder;
import com.payment.util.UserContextHolder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 提现管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/withdrawal")
 
 
public class WithdrawalController {
    
    @Autowired
    private WithdrawalService withdrawalService;
    
    /**
     * 查询商家余额（商家端）
     */
    @RequireAuth
    @GetMapping("/balance")
    public Result<MerchantBalance> getMerchantBalance() {
        Long tenantId = TenantContextHolder.getTenantId();
        MerchantBalance balance = withdrawalService.getMerchantBalance(tenantId);
        return Result.success(balance);
    }
    
    /**
     * 创建提现申请（商家端）
     */
    @RequireAuth
    @PostMapping("/apply")
    public Result<Map<String, Object>> createWithdrawal(@Validated @RequestBody WithdrawalApplyDTO dto) {
        Long tenantId = TenantContextHolder.getTenantId();
        Withdrawal withdrawal = withdrawalService.createWithdrawal(tenantId, dto);
        
        Map<String, Object> result = new HashMap<>();
        result.put("withdrawalId", withdrawal.getId());
        result.put("status", withdrawal.getStatus());
        result.put("message", "提现申请已提交，等待审核");
        
        return Result.success(result);
    }
    
    /**
     * 查询提现记录（商家端）
     */
    @RequireAuth
    @GetMapping("/list")
    public Result<Page<Withdrawal>> listWithdrawals(
               @RequestParam(required = false) Integer status,
               @RequestParam(defaultValue = "1") Integer pageNum,
               @RequestParam(defaultValue = "10") Integer pageSize) {
        Long tenantId = TenantContextHolder.getTenantId();
        
        WithdrawalQueryDTO query = new WithdrawalQueryDTO();
        query.setTenantId(tenantId);
        query.setStatus(status);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        
        Page<Withdrawal> page = withdrawalService.listWithdrawals(query);
        return Result.success(page);
    }
    
    /**
     * 查询所有提现申请（管理端）
     */
    @RequireAuth
    @GetMapping("/admin/list")
    public Result<Page<Withdrawal>> listAllWithdrawals(
               @RequestParam(required = false) Long tenantId,
               @RequestParam(required = false) Integer status,
               @RequestParam(defaultValue = "1") Integer pageNum,
               @RequestParam(defaultValue = "10") Integer pageSize) {
        
        WithdrawalQueryDTO query = new WithdrawalQueryDTO();
        query.setTenantId(tenantId);
        query.setStatus(status);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        
        Page<Withdrawal> page = withdrawalService.listWithdrawals(query);
        return Result.success(page);
    }
    
    /**
     * 审核提现申请（管理端）
     */
    @RequireAuth
    @PostMapping("/admin/approve")
    public Result<Void> approveWithdrawal(@Validated @RequestBody WithdrawalApproveDTO dto) {
        Long approverId = UserContextHolder.getUserId();
        withdrawalService.approveWithdrawal(approverId, dto);
        
        String message = dto.getApproved() ? "提现申请已通过" : "提现申请已拒绝";
        log.info("审核提现申请，approverId={}, withdrawalId={}, approved={}", 
                approverId, dto.getWithdrawalId(), dto.getApproved());
        
        return Result.success();
    }
}
