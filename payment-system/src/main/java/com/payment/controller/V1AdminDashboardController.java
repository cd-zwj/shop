package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.common.Result;
import com.payment.dto.AdminDashboardOverviewVO;
import com.payment.dto.AdminTrendVO;
import com.payment.service.V1AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 平台管理端 - 数据总览控制器。
 * <p>提供管理后台首页所需的信息总览、仪表盘概览和趋势数据查询，接口路径前缀 /v1/admin。</p>
 * <p>需 admin 角色并具备 admin:dashboard 权限。</p>
 */
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class V1AdminDashboardController {

    private final V1AdminService v1AdminService;

    /**
     * 获取管理员基础信息。
     * <p>返回当前登录管理员的基本账户信息。</p>
     * <p>GET /v1/admin/info</p>
     *
     * @return 管理员信息键值对
     */
    @SaCheckPermission(type = "admin", value = "admin:dashboard")
    @GetMapping("/info")
    public Result<Map<String, Object>> getAdminInfo() {
        return Result.success(v1AdminService.getAdminInfo());
    }

    /**
     * 获取仪表盘概览数据。
     * <p>返回平台核心指标汇总，如商户数、用户数、交易额等。</p>
     * <p>GET /v1/admin/dashboard/overview</p>
     *
     * @return 仪表盘概览数据（AdminDashboardOverviewVO）
     */
    @SaCheckPermission(type = "admin", value = "admin:dashboard")
    @GetMapping("/dashboard/overview")
    public Result<AdminDashboardOverviewVO> getOverview() {
        return Result.success(v1AdminService.getDashboardOverview());
    }

    /**
     * 获取平台趋势数据。
     * <p>按指定时间范围和粒度（天/周/月）返回交易、用户等趋势图表数据。</p>
     * <p>GET /v1/admin/dashboard/trend</p>
     *
     * @param startDate   开始日期（可选，格式 yyyy-MM-dd）
     * @param endDate     结束日期（可选，格式 yyyy-MM-dd）
     * @param granularity 数据粒度，默认 DAY，可选 DAY/WEEK/MONTH
     * @return 趋势数据（AdminTrendVO）
     */
    @SaCheckPermission(type = "admin", value = "admin:dashboard")
    @GetMapping("/dashboard/trend")
    public Result<AdminTrendVO> getTrend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "DAY") String granularity) {
        return Result.success(v1AdminService.getTrend(startDate, endDate, granularity));
    }
}
