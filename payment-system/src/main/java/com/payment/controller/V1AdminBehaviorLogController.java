package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.entity.UserBehaviorLog;
import com.payment.service.UserBehaviorLogService;
import com.payment.vo.UserBehaviorLogVO;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户行为日志查询接口。
 */
@RestController
@RequestMapping("/v1/admin/behavior-logs")
@RequiredArgsConstructor
public class V1AdminBehaviorLogController {

    private final UserBehaviorLogService userBehaviorLogService;

    @SaCheckPermission("admin:behaviorlog:list")
    @GetMapping
    public Result<PageResult<UserBehaviorLogVO>> listBehaviorLogs(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String behaviorType) {

        Page<UserBehaviorLog> page = userBehaviorLogService.listByUser(
                userId, tenantId, behaviorType, current, size);
        return Result.success(PageResult.from(page, UserBehaviorLogVO::from));
    }
}
