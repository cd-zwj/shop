package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.annotation.RateLimit;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.*;
import com.payment.entity.MemberPointsLog;
import com.payment.service.MemberPointsAccountService;
import com.payment.service.MerchantRechargeRuleService;
import com.payment.service.MerchantWalletService;
import com.payment.service.AppAssetSummaryService;
import com.payment.service.UnifiedWalletService;
import com.payment.service.WalletRechargeService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.PointsAccountVO;
import com.payment.vo.PointsLogVO;
import com.payment.vo.RechargeRuleVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * C端用户钱包与积分控制器。
 * <p>
 * 提供双钱包系统（统一钱包+商户钱包）的余额查询、交易记录、充值，以及
 * 积分账户查询和积分变动记录功能。
 * <p>
 * 双钱包体系：
 * <ul>
 *   <li>统一钱包（unified）：全局通用钱包，跨商户使用</li>
 *   <li>商户钱包（merchant）：特定商户专属钱包，用于该商户消费</li>
 * </ul>
 * <p>
 * 路径前缀：/v1/app，需要platform用户登录。
 *
 * @author payment-system
 */
@RestController
@RequestMapping("/v1/app")
@RequiredArgsConstructor
@Validated
public class V1AppWalletController {

    private final UnifiedWalletService unifiedWalletService;
    private final MerchantWalletService merchantWalletService;
    private final WalletRechargeService walletRechargeService;
    private final MerchantRechargeRuleService merchantRechargeRuleService;
    private final MemberPointsAccountService memberPointsAccountService;
    private final AppAssetSummaryService appAssetSummaryService;

    /**
     * 查询统一钱包余额。
     * <p>
     * 获取当前用户的统一钱包账户信息，包括余额、冻结金额等。
     *
     * @return 统一钱包账户信息
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/wallets/unified")
    public Result<WalletAccountVO> getUnifiedWallet() {
        return Result.success(unifiedWalletService.getWallet(PlatformSessionHelper.getPlatformUserId()));
    }

    /**
     * 查询统一钱包交易记录。
     * <p>
     * 分页查询统一钱包的充值、消费、退款等交易流水记录。
     *
     * @param current 页码，默认1，必须大于0
     * @param size    每页条数，默认10，必须大于0
     * @return 钱包交易记录分页结果
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/wallets/unified/logs")
    public Result<PageResult<WalletLogVO>> getUnifiedWalletLogs(@RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                                 @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size) {
        Page<WalletLogVO> page = unifiedWalletService.listLogs(PlatformSessionHelper.getPlatformUserId(), current, size);
        return Result.success(PageResult.from(page));
    }

    /**
     * 查询当前用户在各商户下已有资产概览。
     *
     * <p>该接口只读取已有会员/钱包/积分账户，不会因为概览展示创建空账户。</p>
     *
     * @return 商户资产概览列表
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/assets/tenant-summaries")
    public Result<List<AppTenantAssetSummaryVO>> listTenantAssetSummaries() {
        return Result.success(appAssetSummaryService.listTenantAssetSummaries(PlatformSessionHelper.getPlatformUserId()));
    }

    /**
     * 查询当前用户统一资产动态。
     *
     * <p>保留旧版数组响应，仅通过 size 控制返回条数。</p>
     *
     * @param size 返回条数，默认20
     * @return 统一资产动态列表
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/assets/activities")
    public Result<List<AppAssetActivityVO>> listAssetActivities(
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(appAssetSummaryService.listAssetActivities(PlatformSessionHelper.getPlatformUserId(), size));
    }

    /**
     * 分页查询当前用户统一资产动态。
     *
     * @param query 筛选条件及游标
     * @return 统一资产动态分页结果
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/assets/activities/page")
    public Result<AssetActivityPageVO> listAssetActivitiesPage(@Valid @ModelAttribute AssetActivityQueryDTO query) {
        if (query.getSize() == null) {
            query.setSize(20);
        }
        return Result.success(appAssetSummaryService.listAssetActivities(PlatformSessionHelper.getPlatformUserId(), query));
    }

    /**
     * 查询当前用户的受限资产。
     *
     * <p>tenantId 省略时返回当前用户全部商户的锁定券、预占积分和账户冻结摘要。</p>
     *
     * @param tenantId 可选商户ID
     * @return 受限资产明细
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/assets/holds")
    public Result<List<AssetHoldVO>> listAssetHolds(
            @RequestParam(required = false) @Min(value = 1, message = "商户ID必须大于0") Long tenantId) {
        return Result.success(appAssetSummaryService.listAssetHolds(PlatformSessionHelper.getPlatformUserId(), tenantId));
    }

    /**
     * 查询统一钱包充值规则。
     * <p>
     * 获取所有可用的统一钱包充值优惠规则，如充100送10等。
     *
     * @return 充值规则列表
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/wallets/unified/recharge-rules")
    public Result<List<RechargeRuleVO>> listUnifiedRechargeRules() {
        return Result.success(merchantRechargeRuleService.listAllActiveRules().stream()
                .map(RechargeRuleVO::from)
                .collect(Collectors.toList()));
    }

    /**
     * 创建统一钱包充值订单。
     * <p>
     * 用户选择充值金额和充值规则后创建充值订单，返回支付信息。
     * 同一IP每分钟最多发起5次充值请求。
     *
     * @param dto 充值请求DTO，包含充值金额、充值规则ID等
     * @return 充值支付信息（支付链接等）
     */
    @RateLimit(prefix = "app:wallet:recharge:unified", window = 60, maxRequests = 5, includeIp = true, message = "统一钱包充值过于频繁，请稍后再试")
    @SaCheckLogin(type = "platform")
    @PostMapping("/wallets/unified/recharges")
    public Result<RechargePaymentVO> createUnifiedRecharge(@Valid @RequestBody CreateUnifiedWalletRechargeDTO dto) {
        return Result.success(walletRechargeService.createUnifiedRecharge(PlatformSessionHelper.getPlatformUserId(), dto));
    }

    /**
     * 查询商户钱包余额。
     * <p>
     * 获取当前用户在指定商户下的商户钱包账户信息，包括余额、冻结金额等。
     *
     * @param tenantId 商户ID，必须大于0
     * @return 商户钱包账户信息
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/tenants/{tenantId}/wallet")
    public Result<WalletAccountVO> getMerchantWallet(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        return Result.success(merchantWalletService.getWallet(tenantId, PlatformSessionHelper.getPlatformUserId()));
    }

    /**
     * 查询商户钱包交易记录。
     * <p>
     * 分页查询当前用户在指定商户下的钱包交易流水记录。
     *
     * @param tenantId 商户ID，必须大于0
     * @param current  页码，默认1，必须大于0
     * @param size     每页条数，默认10，必须大于0
     * @return 商户钱包交易记录分页结果
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/tenants/{tenantId}/wallet/logs")
    public Result<PageResult<WalletLogVO>> getMerchantWalletLogs(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                                  @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                                  @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size) {
        Page<WalletLogVO> page = merchantWalletService.listLogs(tenantId, PlatformSessionHelper.getPlatformUserId(), current, size);
        return Result.success(PageResult.from(page));
    }

    /**
     * 查询商户钱包充值规则。
     * <p>
     * 获取指定商户下所有可用的充值优惠规则。
     *
     * @param tenantId 商户ID，必须大于0
     * @return 该商户的充值规则列表
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/tenants/{tenantId}/recharge-rules")
    public Result<List<RechargeRuleVO>> listRechargeRules(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        return Result.success(merchantRechargeRuleService.listActiveRules(tenantId).stream()
                .map(RechargeRuleVO::from)
                .collect(Collectors.toList()));
    }

    /**
     * 创建商户钱包充值订单。
     * <p>
     * 用户选择充值金额后为指定商户的钱包创建充值订单，返回支付信息。
     * 同一商户每分钟最多发起5次充值请求。
     *
     * @param tenantId 商户ID，必须大于0
     * @param dto      充值请求DTO，包含充值金额、充值规则ID等
     * @return 充值支付信息（支付链接等）
     */
    @RateLimit(prefix = "app:wallet:recharge:merchant", key = "#tenantId", window = 60, maxRequests = 5, includeIp = true, message = "商户钱包充值过于频繁，请稍后再试")
    @SaCheckLogin(type = "platform")
    @PostMapping("/tenants/{tenantId}/wallet/recharges")
    public Result<RechargePaymentVO> createMerchantRecharge(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                            @Valid @RequestBody CreateMerchantWalletRechargeDTO dto) {
        return Result.success(walletRechargeService.createMerchantRecharge(tenantId, PlatformSessionHelper.getPlatformUserId(), dto));
    }

    /**
     * 查询积分账户信息。
     * <p>
     * 获取当前用户在指定商户下的积分账户概览，包括当前积分总额、
     * 30天内即将过期的积分数量等。
     *
     * @param tenantId 商户ID，必须大于0
     * @return 积分账户信息
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/tenants/{tenantId}/points")
    public Result<PointsAccountVO> getPointsAccount(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        LocalDateTime now = LocalDateTime.now();
        return Result.success(PointsAccountVO.from(
                memberPointsAccountService.getAccount(tenantId, platformUserId),
                memberPointsAccountService.getExpiringPoints(tenantId, platformUserId, now, now.plusDays(30))));
    }

    /**
     * 查询积分变动记录。
     * <p>
     * 分页查询当前用户在指定商户下的积分获取、消费、过期等变动日志。
     *
     * @param tenantId 商户ID，必须大于0
     * @param current  页码，默认1，必须大于0
     * @param size     每页条数，默认10，必须大于0
     * @return 积分变动记录分页结果
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/tenants/{tenantId}/points/logs")
    public Result<PageResult<PointsLogVO>> listPointsLogs(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                               @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                               @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size) {
        Page<MemberPointsLog> page = memberPointsAccountService.listLogs(tenantId, PlatformSessionHelper.getPlatformUserId(), current, size);
        return Result.success(PageResult.from(page, PointsLogVO::from));
    }

}
