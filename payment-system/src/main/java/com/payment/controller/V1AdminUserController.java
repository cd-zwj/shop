package com.payment.controller;

import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.AdminPlatformUserVO;
import com.payment.dto.PermissionVO;
import com.payment.dto.UserPermissionDTO;
import com.payment.dto.UserPermissionVO;
import com.payment.entity.Permission;
import com.payment.service.V1AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * v1 管理端用户与权限接口。
 */
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class V1AdminUserController {

    private final V1AdminService v1AdminService;

    @SaCheckPermission(value = {"admin:user:list", "admin:dashboard"}, mode = SaMode.OR)
    @GetMapping("/users")
    public Result<PageResult<AdminPlatformUserVO>> listUsers(@RequestParam(defaultValue = "1") Integer current,
                                                              @RequestParam(defaultValue = "10") Integer size,
                                                              @RequestParam(required = false) String keyword,
                                                              @RequestParam(required = false) Integer status) {
        Page<AdminPlatformUserVO> page = v1AdminService.listPlatformUsers(current, size, keyword, status);
        return Result.success(PageResult.from(page));
    }

    @SaCheckPermission(value = {"admin:user:list", "admin:dashboard"}, mode = SaMode.OR)
    @GetMapping("/users/{userId}")
    public Result<AdminPlatformUserVO> getUserDetail(@PathVariable Long userId) {
        return Result.success(v1AdminService.getPlatformUserDetail(userId));
    }

    @SaCheckPermission(value = {"admin:user:update", "admin:dashboard"}, mode = SaMode.OR)
    @PutMapping("/users/{userId}/enable")
    public Result<Void> enableUser(@PathVariable Long userId) {
        v1AdminService.enablePlatformUser(userId);
        return Result.success();
    }

    @SaCheckPermission(value = {"admin:user:update", "admin:dashboard"}, mode = SaMode.OR)
    @PutMapping("/users/{userId}/disable")
    public Result<Void> disableUser(@PathVariable Long userId) {
        v1AdminService.disablePlatformUser(userId);
        return Result.success();
    }

    @SaCheckPermission("admin:permission:list")
    @GetMapping("/permissions")
    public Result<Map<String, List<PermissionVO>>> listPermissions() {
        Map<String, List<Permission>> raw = v1AdminService.listPermissions();
        Map<String, List<PermissionVO>> voMap = raw.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().map(p -> {
                            PermissionVO vo = new PermissionVO();
                            BeanUtils.copyProperties(p, vo);
                            return vo;
                        }).collect(Collectors.toList())
                ));
        return Result.success(voMap);
    }

    @SaCheckPermission("admin:user:permission")
    @GetMapping("/users/{userId}/permissions")
    public Result<UserPermissionVO> getUserPermissions(@PathVariable Long userId) {
        return Result.success(v1AdminService.getUserPermissions(userId));
    }

    @SaCheckPermission("admin:user:permission")
    @PutMapping("/users/{userId}/permissions")
    public Result<Void> setUserPermissions(@PathVariable Long userId, @RequestBody UserPermissionDTO dto) {
        v1AdminService.setUserPermissions(userId, dto);
        return Result.success();
    }

    @SaCheckPermission("admin:user:permission")
    @DeleteMapping("/users/{userId}/permissions/{permissionId}")
    public Result<Void> removeUserPermission(@PathVariable Long userId, @PathVariable Long permissionId) {
        v1AdminService.removeUserPermission(userId, permissionId);
        return Result.success();
    }
}
