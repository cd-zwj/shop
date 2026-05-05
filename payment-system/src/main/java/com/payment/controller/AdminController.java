package com.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.payment.common.BusinessException;
import com.payment.common.Result;
import com.payment.dto.LoginDTO;
import com.payment.dto.MerchantQueryDTO;
import com.payment.dto.MerchantDetailVO;
import com.payment.entity.Tenant;
import com.payment.entity.User;
import com.payment.entity.Withdrawal;
import com.payment.service.MerchantService;
import com.payment.service.UserService;
import com.payment.service.WithdrawalService;
import com.payment.service.UserPermissionService;
import com.payment.mapper.PermissionMapper;
import com.payment.entity.Permission;
import com.payment.dto.UserPermissionDTO;
import com.payment.dto.UserPermissionVO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 平台管理员控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")

public class AdminController {
    
    @Autowired
    private MerchantService merchantService;
    
    @Autowired
    private WithdrawalService withdrawalService;
    @Autowired
    private UserService userService;

    @Autowired
    private UserPermissionService userPermissionService;

    @Autowired
    private PermissionMapper permissionMapper;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername(username);
        loginDTO.setPassword(password);
        String loginadmin = userService.loginadmin(loginDTO);
        log.info("管理员登录，username: {}", username);
        return Result.success(loginadmin);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }
    
    /**
     * 获取管理员信息
     */
    @SaCheckPermission("admin:dashboard")
    @GetMapping("/info")

    public Result<Map<String, Object>> getAdminInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("id", 1);
        info.put("username", "admin");
        info.put("role", "ADMIN");
        
        return Result.success(info);
    }
    
    /**
     * 平台数据概览
     */
    @SaCheckPermission("admin:dashboard")
    @GetMapping("/dashboard/stats")

    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = merchantService.getDashboardStats();
        return Result.success(stats);
    }
    
    /**
     * 商家注册趋势
     */
    @SaCheckPermission("admin:dashboard")
    @GetMapping("/dashboard/merchant-trend")
    public Result<Map<String, Object>> getMerchantTrend() {
        Map<String, Object> data = merchantService.getMerchantTrend();
        return Result.success(data);
    }
    
    /**
     * 平台销售趋势
     */
    @SaCheckPermission("admin:dashboard")
    @GetMapping("/dashboard/sales-trend")

    public Result<Map<String, Object>> getSalesTrend() {
        Map<String, Object> data = merchantService.getSalesTrend();
        return Result.success(data);
    }
    
    /**
     * 商家列表
     */
    @SaCheckPermission("admin:merchant:list")
    @GetMapping("/merchants")
    public Result<Page<com.payment.dto.MerchantListVO>> listMerchants(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status) {
        
        MerchantQueryDTO query = new MerchantQueryDTO();
        query.setName(name);
        query.setStatus(status);
        query.setPageNum(current);
        query.setPageSize(size);
        
        Page<Tenant> tenantPage = merchantService.listMerchants(query);
        
        // 转换为VO
        Page<com.payment.dto.MerchantListVO> voPage = new Page<>(tenantPage.getCurrent(), tenantPage.getSize(), tenantPage.getTotal());
        java.util.List<com.payment.dto.MerchantListVO> voList = tenantPage.getRecords().stream()
                .map(tenant -> {
                    com.payment.dto.MerchantListVO vo = new com.payment.dto.MerchantListVO();
                    vo.setId(tenant.getId());
                    vo.setTenantCode(tenant.getTenantCode());
                    vo.setName(tenant.getName());
                    vo.setContactName(tenant.getContact());
                    vo.setContactPhone(tenant.getPhone());
                    vo.setStatus(tenant.getStatus());
                    vo.setCreateTime(tenant.getCreateTime());
                    return vo;
                })
                .collect(java.util.stream.Collectors.toList());
        voPage.setRecords(voList);
        
        return Result.success(voPage);
    }
    
    /**
     * 商家详情
     */
    @SaCheckPermission("admin:merchant:detail")
    @GetMapping("/merchant/{id}")
    public Result<Map<String, Object>> getMerchantDetail(@PathVariable Long id) {
        log.info("查询商家详情，id: {}", id);
        
        MerchantDetailVO detail = merchantService.getMerchantDetail(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("merchant", detail.getMerchant());
        result.put("stats", detail.getStats());
        result.put("balance", detail.getBalance());
        
        return Result.success(result);
    }
    
    /**
     * 启用商家
     */
    @SaCheckPermission("admin:merchant:enable")
    @PutMapping("/merchant/{id}/enable")

    public Result<Void> enableMerchant( @PathVariable Long id) {
        log.info("启用商家，id: {}", id);
        
        merchantService.enableMerchant(id);
        
        return Result.success();
    }
    
    /**
     * 禁用商家
     */
    @SaCheckPermission("admin:merchant:disable")
    @PutMapping("/merchant/{id}/disable")

    public Result<Void> disableMerchant( @PathVariable Long id) {
        log.info("禁用商家，id: {}", id);
        
        merchantService.disableMerchant(id);
        
        return Result.success();
    }
    
    /**
     * 提现申请列表
     */
    @SaCheckPermission("admin:withdrawal:list")
    @GetMapping("/withdrawals")

    public Result<Page<com.payment.dto.WithdrawalVO>> listWithdrawals(
           @RequestParam(defaultValue = "1") Integer current,
           @RequestParam(defaultValue = "10") Integer size,
          @RequestParam(required = false) String merchantName,
          @RequestParam(required = false) Integer status,
          @RequestParam(required = false) String startDate,
          @RequestParam(required = false) String endDate) {
        
        Page<com.payment.dto.WithdrawalVO> page = withdrawalService.listWithdrawalsForAdmin(
            current, size, merchantName, status, startDate, endDate);
        
        return Result.success(page);
    }
    
    /**
     * 审核通过提现申请
     */
    @SaCheckPermission("admin:withdrawal:approve")
    @PutMapping("/withdrawal/{id}/approve")
    public Result<Void> approveWithdrawal(@PathVariable Long id) {
        log.info("审核通过提现申请，id: {}", id);
        
        withdrawalService.approveWithdrawal(id);
        
        return Result.success();
    }
    
    /**
     * 拒绝提现申请
     */
    @SaCheckPermission("admin:withdrawal:reject")
    @PutMapping("/withdrawal/{id}/reject")
    public Result<Void> rejectWithdrawal(@PathVariable Long id,
            @RequestBody Map<String, String> data) {
        String reason = data.get("reason");
        log.info("拒绝提现申请，id: {}, reason: {}", id, reason);

        withdrawalService.rejectWithdrawal(id, reason);

        return Result.success();
    }

    /**
     * 获取所有权限列表（按模块分组）
     */
    @SaCheckPermission("admin:permission:list")
    @GetMapping("/permissions")
    public Result<Map<String, java.util.List<Permission>>> listAllPermissions() {
        java.util.List<Permission> list = permissionMapper.selectList(null);
        // Group by module
        Map<String, java.util.List<Permission>> grouped = list.stream()
            .collect(java.util.stream.Collectors.groupingBy(Permission::getModule));
        return Result.success(grouped);
    }

    /**
     * 获取用户权限详情
     */
    @SaCheckPermission("admin:user:permission")
    @GetMapping("/user/{userId}/permissions")
    public Result<UserPermissionVO> getUserPermissions(@PathVariable Long userId) {
        return Result.success(userPermissionService.getUserPermissions(userId));
    }

    /**
     * 设置用户额外权限
     */
    @SaCheckPermission("admin:user:permission")
    @PostMapping("/user/{userId}/permissions")
    public Result<Void> setUserPermissions(@PathVariable Long userId, @RequestBody UserPermissionDTO dto) {
        userPermissionService.setUserPermissions(userId, dto.getPermissionIds());
        return Result.success();
    }

    /**
     * 移除用户某个权限
     */
    @SaCheckPermission("admin:user:permission")
    @DeleteMapping("/user/{userId}/permissions/{permissionId}")
    public Result<Void> removeUserPermission(@PathVariable Long userId, @PathVariable Long permissionId) {
        userPermissionService.revokePermission(userId, permissionId);
        return Result.success();
    }
}
