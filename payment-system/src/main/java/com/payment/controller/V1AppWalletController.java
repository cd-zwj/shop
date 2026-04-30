package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.Result;
import com.payment.dto.*;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.MemberPointsLog;
import com.payment.entity.MerchantRechargeRule;
import com.payment.service.MemberPointsAccountService;
import com.payment.service.MerchantRechargeRuleService;
import com.payment.service.MerchantWalletService;
import com.payment.service.UnifiedWalletService;
import com.payment.service.WalletRechargeService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * v1 用户端钱包与积分接口。
 */
@RestController
@RequestMapping("/v1/app")
@RequiredArgsConstructor
public class V1AppWalletController {

    private final UnifiedWalletService unifiedWalletService;
    private final MerchantWalletService merchantWalletService;
    private final WalletRechargeService walletRechargeService;
    private final MerchantRechargeRuleService merchantRechargeRuleService;
    private final MemberPointsAccountService memberPointsAccountService;

    @SaCheckLogin
    @GetMapping("/wallets/unified")
    public Result<WalletAccountVO> getUnifiedWallet() {
        return Result.success(unifiedWalletService.getWallet(PlatformSessionHelper.getPlatformUserId()));
    }

    @SaCheckLogin
    @GetMapping("/wallets/unified/logs")
    public Result<Page<WalletLogVO>> getUnifiedWalletLogs(@RequestParam(defaultValue = "1") Integer current,
                                                          @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(unifiedWalletService.listLogs(PlatformSessionHelper.getPlatformUserId(), current, size));
    }

    @SaCheckLogin
    @PostMapping("/wallets/unified/recharges")
    public Result<RechargePaymentVO> createUnifiedRecharge(@Valid @RequestBody CreateUnifiedWalletRechargeDTO dto) {
        return Result.success(walletRechargeService.createUnifiedRecharge(PlatformSessionHelper.getPlatformUserId(), dto));
    }

    @SaCheckLogin
    @GetMapping("/tenants/{tenantId}/wallet")
    public Result<WalletAccountVO> getMerchantWallet(@PathVariable Long tenantId) {
        return Result.success(merchantWalletService.getWallet(tenantId, PlatformSessionHelper.getPlatformUserId()));
    }

    @SaCheckLogin
    @GetMapping("/tenants/{tenantId}/wallet/logs")
    public Result<Page<WalletLogVO>> getMerchantWalletLogs(@PathVariable Long tenantId,
                                                           @RequestParam(defaultValue = "1") Integer current,
                                                           @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(merchantWalletService.listLogs(tenantId, PlatformSessionHelper.getPlatformUserId(), current, size));
    }

    @SaCheckLogin
    @GetMapping("/tenants/{tenantId}/recharge-rules")
    public Result<List<MerchantRechargeRule>> listRechargeRules(@PathVariable Long tenantId) {
        return Result.success(merchantRechargeRuleService.listActiveRules(tenantId));
    }

    @SaCheckLogin
    @PostMapping("/tenants/{tenantId}/wallet/recharges")
    public Result<RechargePaymentVO> createMerchantRecharge(@PathVariable Long tenantId,
                                                            @Valid @RequestBody CreateMerchantWalletRechargeDTO dto) {
        return Result.success(walletRechargeService.createMerchantRecharge(tenantId, PlatformSessionHelper.getPlatformUserId(), dto));
    }

    @SaCheckLogin
    @GetMapping("/tenants/{tenantId}/points")
    public Result<MemberPointsAccount> getPointsAccount(@PathVariable Long tenantId) {
        return Result.success(memberPointsAccountService.getAccount(tenantId, PlatformSessionHelper.getPlatformUserId()));
    }

    @SaCheckLogin
    @GetMapping("/tenants/{tenantId}/points/logs")
    public Result<Page<MemberPointsLog>> listPointsLogs(@PathVariable Long tenantId,
                                                        @RequestParam(defaultValue = "1") Integer current,
                                                        @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(memberPointsAccountService.listLogs(tenantId, PlatformSessionHelper.getPlatformUserId(), current, size));
    }
}
