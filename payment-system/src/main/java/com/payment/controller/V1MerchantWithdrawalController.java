package com.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.constant.MerchantPermission;
import com.payment.dto.V1MerchantBalanceVO;
import com.payment.dto.WithdrawalApplyDTO;
import com.payment.dto.WithdrawalQueryDTO;
import com.payment.dto.WithdrawalVO;
import com.payment.entity.MerchantBalance;
import com.payment.entity.Withdrawal;
import com.payment.service.WithdrawalService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 商户端提现管理控制器（Merchant 端）。
 * <p>提供商户钱包余额查看、提现记录查询和提现申请提交等功能。
 * 需要商户角色登录，并通过商户员工本地权限矩阵控制访问。</p>
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/withdrawals")
@RequiredArgsConstructor
@SaCheckLogin(type = "merchant")
public class V1MerchantWithdrawalController {

    private final V1MerchantSupportService v1MerchantSupportService;
    private final WithdrawalService withdrawalService;

    @GetMapping("/balance")
    public Result<V1MerchantBalanceVO> getBalance(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        v1MerchantSupportService.requirePermission(tenantId, PlatformSessionHelper.getPlatformUserId(), MerchantPermission.WITHDRAWAL_MANAGE);
        MerchantBalance balance = withdrawalService.getMerchantBalance(tenantId);
        V1MerchantBalanceVO vo = new V1MerchantBalanceVO();
        vo.setTenantId(tenantId);
        vo.setAvailableBalance(balance == null ? BigDecimal.ZERO : balance.getBalance());
        vo.setFrozenBalance(balance == null ? BigDecimal.ZERO : balance.getFrozenBalance());
        vo.setTotalIncome(balance == null ? BigDecimal.ZERO : balance.getTotalIncome());
        vo.setTotalWithdrawal(balance == null ? BigDecimal.ZERO : balance.getTotalWithdrawal());
        return Result.success(vo);
    }

    @GetMapping
    public Result<PageResult<WithdrawalVO>> listWithdrawals(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                           @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                           @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
                                                           @RequestParam(required = false) Integer status) {
        v1MerchantSupportService.requirePermission(tenantId, PlatformSessionHelper.getPlatformUserId(), MerchantPermission.WITHDRAWAL_MANAGE);
        WithdrawalQueryDTO queryDTO = new WithdrawalQueryDTO();
        queryDTO.setTenantId(tenantId);
        queryDTO.setStatus(status);
        queryDTO.setPageNum(current);
        queryDTO.setPageSize(size);
        Page<Withdrawal> page = withdrawalService.listWithdrawals(queryDTO);
        return Result.success(PageResult.from(page, e -> {
            WithdrawalVO vo = new WithdrawalVO();
            BeanUtils.copyProperties(e, vo);
            return vo;
        }));
    }

    @PostMapping
    public Result<WithdrawalVO> createWithdrawal(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @Valid @RequestBody WithdrawalApplyDTO dto) {
        v1MerchantSupportService.requirePermission(tenantId, PlatformSessionHelper.getPlatformUserId(), MerchantPermission.WITHDRAWAL_MANAGE);
        Withdrawal entity = withdrawalService.createWithdrawal(tenantId, dto);
        WithdrawalVO vo = new WithdrawalVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }
}
