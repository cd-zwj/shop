package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.WithdrawalVO;
import com.payment.service.V1AdminService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * v1 管理端提现审核接口。
 */
@RestController
@RequestMapping("/v1/admin/withdrawals")
@RequiredArgsConstructor
public class V1AdminWithdrawalController {

    private final V1AdminService v1AdminService;

    @SaCheckPermission("admin:withdrawal:list")
    @GetMapping
    public Result<PageResult<WithdrawalVO>> listWithdrawals(@RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                             @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
                                                             @RequestParam(required = false) String merchantName,
                                                             @RequestParam(required = false) Integer status,
                                                             @RequestParam(required = false) String startDate,
                                                             @RequestParam(required = false) String endDate) {
        Page<WithdrawalVO> page = v1AdminService.listWithdrawals(current, size, merchantName, status, startDate, endDate);
        return Result.success(PageResult.from(page));
    }

    @SaCheckPermission("admin:withdrawal:approve")
    @PutMapping("/{withdrawalId}/approve")
    public Result<Void> approve(@PathVariable @Min(value = 1, message = "ID必须大于0") Long withdrawalId) {
        v1AdminService.approveWithdrawal(withdrawalId);
        return Result.success();
    }

    @SaCheckPermission("admin:withdrawal:reject")
    @PutMapping("/{withdrawalId}/reject")
    public Result<Void> reject(@PathVariable @Min(value = 1, message = "ID必须大于0") Long withdrawalId, @RequestBody Map<String, String> body) {
        v1AdminService.rejectWithdrawal(withdrawalId, body.get("reason"));
        return Result.success();
    }
}
