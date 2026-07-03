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
 * C端用户成长值控制器。
 * <p>
 * 提供会员成长值概览查询和成长值变动日志查询接口。
 * 成长值是用户在指定商户下的会员等级提升依据，消费、签到等行为可获得成长值。
 * <p>
 * 路径前缀：/v1/app/tenants/{tenantId}/growth，需要platform用户登录。
 *
 * @author payment-system
 */
@RestController
@RequestMapping("/v1/app/tenants/{tenantId}/growth")
@RequiredArgsConstructor
public class V1AppGrowthController {

    private final MemberGrowthService memberGrowthService;

    /**
     * 查询当前用户成长值概览。
     * <p>
     * 获取当前用户在指定商户下的成长值总额、当前会员等级、
     * 升级到下一等级所需的成长值阈值等信息。
     *
     * @param tenantId 商户ID，必须大于0
     * @return 成长值账户概览信息
     */
    @SaCheckLogin(type = "platform")
    @GetMapping
    public Result<MemberGrowthAccountVO> getGrowthAccount(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        return Result.success(memberGrowthService.getGrowthAccount(platformUserId, tenantId));
    }

    /**
     * 查询成长值变动日志。
     * <p>
     * 分页查询当前用户在指定商户下的成长值变动记录，
     * 包括消费获得、签到获得、等级调整等变动类型。
     *
     * @param tenantId 商户ID，必须大于0
     * @param current  页码，默认1，必须大于0
     * @param size     每页条数，默认10，必须大于0
     * @return 成长值变动日志分页结果
     */
    @SaCheckLogin(type = "platform")
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
