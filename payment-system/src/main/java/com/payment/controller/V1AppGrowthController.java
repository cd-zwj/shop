package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.entity.MemberGrowthLog;
import com.payment.service.MemberGrowthService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.MemberGrowthAccountVO;
import com.payment.vo.MemberGrowthLogVO;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * v1 用户端成长值接口。
 */
@RestController
@RequestMapping("/v1/app/tenants/{tenantId}/growth")
@RequiredArgsConstructor
public class V1AppGrowthController {

    private final MemberGrowthService memberGrowthService;

    /**
     * 查询当前成长值概览（总额 + 等级 + 下一级阈值）。
     */
    @SaCheckLogin
    @GetMapping
    public Result<MemberGrowthAccountVO> getGrowthAccount(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        return Result.success(memberGrowthService.getGrowthAccount(platformUserId, tenantId));
    }

    /**
     * 分页查询成长值变动日志。
     */
    @SaCheckLogin
    @GetMapping("/logs")
    public Result<PageResult<MemberGrowthLogVO>> listGrowthLogs(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        Page<MemberGrowthLog> page = memberGrowthService.listGrowthLogs(platformUserId, tenantId, current, size);
        return Result.success(PageResult.from(page, MemberGrowthLogVO::from));
    }
}
