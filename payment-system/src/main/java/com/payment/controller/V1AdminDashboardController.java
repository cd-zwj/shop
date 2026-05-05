package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.common.Result;
import com.payment.dto.AdminDashboardOverviewVO;
import com.payment.service.V1AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * v1 管理端总览接口。
 */
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class V1AdminDashboardController {

    private final V1AdminService v1AdminService;

    @SaCheckPermission("admin:dashboard")
    @GetMapping("/info")
    public Result<Map<String, Object>> getAdminInfo() {
        return Result.success(v1AdminService.getAdminInfo());
    }

    @SaCheckPermission("admin:dashboard")
    @GetMapping("/dashboard/overview")
    public Result<AdminDashboardOverviewVO> getOverview() {
        return Result.success(v1AdminService.getDashboardOverview());
    }
}
