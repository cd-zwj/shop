package com.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.Result;
import com.payment.dto.V1MerchantBalanceVO;
import com.payment.dto.WithdrawalApplyDTO;
import com.payment.dto.WithdrawalQueryDTO;
import com.payment.entity.MerchantBalance;
import com.payment.entity.Withdrawal;
import com.payment.service.WithdrawalService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/withdrawals")
@RequiredArgsConstructor
public class V1MerchantWithdrawalController {

    private final V1MerchantSupportService v1MerchantSupportService;
    private final WithdrawalService withdrawalService;

    @GetMapping("/balance")
    public Result<V1MerchantBalanceVO> getBalance(@PathVariable Long tenantId) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
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
    public Result<Page<Withdrawal>> listWithdrawals(@PathVariable Long tenantId,
                                                    @RequestParam(defaultValue = "1") Integer current,
                                                    @RequestParam(defaultValue = "10") Integer size,
                                                    @RequestParam(required = false) Integer status) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        WithdrawalQueryDTO queryDTO = new WithdrawalQueryDTO();
        queryDTO.setTenantId(tenantId);
        queryDTO.setStatus(status);
        queryDTO.setPageNum(current);
        queryDTO.setPageSize(size);
        return Result.success(withdrawalService.listWithdrawals(queryDTO));
    }

    @PostMapping
    public Result<Withdrawal> createWithdrawal(@PathVariable Long tenantId, @Valid @RequestBody WithdrawalApplyDTO dto) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        return Result.success(withdrawalService.createWithdrawal(tenantId, dto));
    }
}
