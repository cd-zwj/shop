package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.Result;
import com.payment.dto.WithdrawalVO;
import com.payment.service.V1AdminService;
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
    public Result<Page<WithdrawalVO>> listWithdrawals(@RequestParam(defaultValue = "1") Integer current,
                                                      @RequestParam(defaultValue = "10") Integer size,
                                                      @RequestParam(required = false) String merchantName,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false) String startDate,
                                                      @RequestParam(required = false) String endDate) {
        return Result.success(v1AdminService.listWithdrawals(current, size, merchantName, status, startDate, endDate));
    }

    @SaCheckPermission("admin:withdrawal:approve")
    @PutMapping("/{withdrawalId}/approve")
    public Result<Void> approve(@PathVariable Long withdrawalId) {
        v1AdminService.approveWithdrawal(withdrawalId);
        return Result.success();
    }

    @SaCheckPermission("admin:withdrawal:reject")
    @PutMapping("/{withdrawalId}/reject")
    public Result<Void> reject(@PathVariable Long withdrawalId, @RequestBody Map<String, String> body) {
        v1AdminService.rejectWithdrawal(withdrawalId, body.get("reason"));
        return Result.success();
    }
}
