package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.CompensationTaskVO;
import com.payment.dto.RetryTaskVO;
import com.payment.service.CompensationTaskService;
import com.payment.service.RetryTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端 — 补偿任务与重试任务管理。
 *
 * 权限拆分：
 * - admin:compensation:list   — 查询（只读）
 * - admin:compensation:operate — 重试/取消（写操作）
 */
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class V1AdminCompensationController {

    private final CompensationTaskService compensationTaskService;
    private final RetryTaskService retryTaskService;

    /* ---------- CompensationTask ---------- */

    @SaCheckPermission(type = "admin", value = {"admin:compensation:list", "admin:dashboard"}, mode = SaMode.OR)
    @GetMapping("/compensation-tasks")
    public Result<PageResult<CompensationTaskVO>> listCompensationTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String bizType,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(PageResult.from(
                compensationTaskService.list(status, bizType, current, size),
                CompensationTaskVO::from));
    }

    @SaCheckPermission(type = "admin", value = "admin:compensation:operate")
    @PostMapping("/compensation-tasks/{id}/retry")
    public Result<Void> retryCompensationTask(@PathVariable Long id) {
        compensationTaskService.retry(id);
        return Result.success();
    }

    @SaCheckPermission(type = "admin", value = "admin:compensation:operate")
    @PostMapping("/compensation-tasks/{id}/cancel")
    public Result<Void> cancelCompensationTask(@PathVariable Long id) {
        compensationTaskService.cancel(id);
        return Result.success();
    }

    /* ---------- RetryTask ---------- */

    @SaCheckPermission(type = "admin", value = {"admin:compensation:list", "admin:dashboard"}, mode = SaMode.OR)
    @GetMapping("/retry-tasks")
    public Result<PageResult<RetryTaskVO>> listRetryTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(PageResult.from(
                retryTaskService.list(status, taskType, current, size),
                RetryTaskVO::from));
    }

    @SaCheckPermission(type = "admin", value = "admin:compensation:operate")
    @PostMapping("/retry-tasks/{id}/retry")
    public Result<Void> retryRetryTask(@PathVariable Long id) {
        retryTaskService.retry(id);
        return Result.success();
    }

    @SaCheckPermission(type = "admin", value = "admin:compensation:operate")
    @PostMapping("/retry-tasks/{id}/cancel")
    public Result<Void> cancelRetryTask(@PathVariable Long id) {
        retryTaskService.cancel(id);
        return Result.success();
    }
}
