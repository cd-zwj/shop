package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.*;
import com.payment.entity.MemberPointsLog;
import com.payment.entity.ExchangeProduct;
import com.payment.service.MemberPointsAccountService;
import com.payment.service.MerchantRechargeRuleService;
import com.payment.service.MerchantWalletService;
import com.payment.service.PointsService;
import com.payment.service.UnifiedWalletService;
import com.payment.service.WalletRechargeService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.PointsAccountVO;
import com.payment.vo.PointsLogVO;
import com.payment.vo.RechargeRuleVO;
import org.springframework.beans.BeanUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final PointsService pointsService;

    @SaCheckLogin
    @GetMapping("/wallets/unified")
    public Result<WalletAccountVO> getUnifiedWallet() {
        return Result.success(unifiedWalletService.getWallet(PlatformSessionHelper.getPlatformUserId()));
    }

    @SaCheckLogin
    @GetMapping("/wallets/unified/logs")
    public Result<PageResult<WalletLogVO>> getUnifiedWalletLogs(@RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                                 @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size) {
        Page<WalletLogVO> page = unifiedWalletService.listLogs(PlatformSessionHelper.getPlatformUserId(), current, size);
        return Result.success(PageResult.from(page));
    }

    @SaCheckLogin
    @GetMapping("/wallets/unified/recharge-rules")
    public Result<List<RechargeRuleVO>> listUnifiedRechargeRules() {
        return Result.success(merchantRechargeRuleService.listAllActiveRules().stream()
                .map(RechargeRuleVO::from)
                .collect(Collectors.toList()));
    }

    @SaCheckLogin
    @PostMapping("/wallets/unified/recharges")
    public Result<RechargePaymentVO> createUnifiedRecharge(@Valid @RequestBody CreateUnifiedWalletRechargeDTO dto) {
        return Result.success(walletRechargeService.createUnifiedRecharge(PlatformSessionHelper.getPlatformUserId(), dto));
    }

    @SaCheckLogin
    @GetMapping("/tenants/{tenantId}/wallet")
    public Result<WalletAccountVO> getMerchantWallet(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        return Result.success(merchantWalletService.getWallet(tenantId, PlatformSessionHelper.getPlatformUserId()));
    }

    @SaCheckLogin
    @GetMapping("/tenants/{tenantId}/wallet/logs")
    public Result<PageResult<WalletLogVO>> getMerchantWalletLogs(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                                  @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                                  @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size) {
        Page<WalletLogVO> page = merchantWalletService.listLogs(tenantId, PlatformSessionHelper.getPlatformUserId(), current, size);
        return Result.success(PageResult.from(page));
    }

    @SaCheckLogin
    @GetMapping("/tenants/{tenantId}/recharge-rules")
    public Result<List<RechargeRuleVO>> listRechargeRules(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        return Result.success(merchantRechargeRuleService.listActiveRules(tenantId).stream()
                .map(RechargeRuleVO::from)
                .collect(Collectors.toList()));
    }

    @SaCheckLogin
    @PostMapping("/tenants/{tenantId}/wallet/recharges")
    public Result<RechargePaymentVO> createMerchantRecharge(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                            @Valid @RequestBody CreateMerchantWalletRechargeDTO dto) {
        return Result.success(walletRechargeService.createMerchantRecharge(tenantId, PlatformSessionHelper.getPlatformUserId(), dto));
    }

    @SaCheckLogin
    @GetMapping("/tenants/{tenantId}/points")
    public Result<PointsAccountVO> getPointsAccount(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        return Result.success(PointsAccountVO.from(
                memberPointsAccountService.getAccount(tenantId, PlatformSessionHelper.getPlatformUserId())));
    }

    @SaCheckLogin
    @GetMapping("/tenants/{tenantId}/points/logs")
    public Result<PageResult<PointsLogVO>> listPointsLogs(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                               @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                               @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size) {
        Page<MemberPointsLog> page = memberPointsAccountService.listLogs(tenantId, PlatformSessionHelper.getPlatformUserId(), current, size);
        return Result.success(PageResult.from(page, PointsLogVO::from));
    }

    @SaCheckLogin
    @GetMapping("/tenants/{tenantId}/points/exchange/products")
    public Result<List<ExchangeProductVO>> listExchangeProducts(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        return Result.success(pointsService.listExchangeProducts(tenantId).stream()
                .map(e -> { ExchangeProductVO vo = new ExchangeProductVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    @SaCheckLogin
    @PostMapping("/tenants/{tenantId}/points/exchange/{exchangeProductId}")
    public Result<Map<String, String>> exchangeProduct(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                       @PathVariable @Min(value = 1, message = "ID必须大于0") Long exchangeProductId) {
        String orderNo = pointsService.exchangeProduct(PlatformSessionHelper.getPlatformUserId(), exchangeProductId);
        return Result.success(Map.of("orderNo", orderNo, "message", "兑换成功"));
    }
}
