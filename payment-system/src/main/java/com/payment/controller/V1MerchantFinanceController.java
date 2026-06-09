package com.payment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.Result;
import com.payment.dto.MerchantRechargeRuleVO;
import com.payment.dto.V1MerchantBalanceVO;
import com.payment.dto.V1MerchantPointsRuleDTO;
import com.payment.dto.V1MerchantRechargeRuleDTO;
import com.payment.entity.MerchantBalance;
import com.payment.entity.MerchantRechargeRule;
import com.payment.entity.PointsRule;
import com.payment.mapper.MerchantBalanceMapper;
import com.payment.mapper.PointsRuleMapper;
import com.payment.service.MerchantRechargeRuleService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}")
@RequiredArgsConstructor
public class V1MerchantFinanceController {

    private final V1MerchantSupportService v1MerchantSupportService;
    private final MerchantBalanceMapper merchantBalanceMapper;
    private final PointsRuleMapper pointsRuleMapper;
    private final MerchantRechargeRuleService merchantRechargeRuleService;

    @GetMapping("/wallet-summary")
    public Result<V1MerchantBalanceVO> getWalletSummary(@PathVariable Long tenantId) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        MerchantBalance balance = merchantBalanceMapper.selectOne(new LambdaQueryWrapper<MerchantBalance>()
                .eq(MerchantBalance::getTenantId, tenantId)
                .eq(MerchantBalance::getDeleted, 0));

        V1MerchantBalanceVO vo = new V1MerchantBalanceVO();
        vo.setTenantId(tenantId);
        vo.setAvailableBalance(balance == null ? java.math.BigDecimal.ZERO : balance.getBalance());
        vo.setFrozenBalance(balance == null ? java.math.BigDecimal.ZERO : balance.getFrozenBalance());
        vo.setTotalIncome(balance == null ? java.math.BigDecimal.ZERO : balance.getTotalIncome());
        vo.setTotalWithdrawal(balance == null ? java.math.BigDecimal.ZERO : balance.getTotalWithdrawal());
        return Result.success(vo);
    }

    @GetMapping("/points-rule")
    public Result<V1MerchantPointsRuleDTO> getPointsRule(@PathVariable Long tenantId) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        PointsRule rule = pointsRuleMapper.selectOne(new LambdaQueryWrapper<PointsRule>()
                .eq(PointsRule::getTenantId, tenantId)
                .eq(PointsRule::getDeleted, 0));
        V1MerchantPointsRuleDTO dto = new V1MerchantPointsRuleDTO();
        dto.setPointsRatio(rule == null ? 0 : rule.getPointsRatio());
        dto.setEnabled(rule != null && rule.getEnabled() != null && rule.getEnabled() == 1);
        return Result.success(dto);
    }

    @PutMapping("/points-rule")
    public Result<Void> updatePointsRule(@PathVariable Long tenantId, @Valid @RequestBody V1MerchantPointsRuleDTO dto) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        PointsRule rule = pointsRuleMapper.selectOne(new LambdaQueryWrapper<PointsRule>()
                .eq(PointsRule::getTenantId, tenantId)
                .eq(PointsRule::getDeleted, 0));
        if (rule == null) {
            rule = new PointsRule();
            rule.setTenantId(tenantId);
            rule.setDeleted(0);
            rule.setCreateTime(LocalDateTime.now());
        }
        rule.setPointsRatio(dto.getPointsRatio());
        rule.setEnabled(Boolean.TRUE.equals(dto.getEnabled()) ? 1 : 0);
        rule.setUpdateTime(LocalDateTime.now());
        if (rule.getId() == null) {
            pointsRuleMapper.insert(rule);
        } else {
            pointsRuleMapper.updateById(rule);
        }
        return Result.success();
    }

    @GetMapping("/recharge-rules")
    public Result<List<MerchantRechargeRuleVO>> listRechargeRules(@PathVariable Long tenantId) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        return Result.success(merchantRechargeRuleService.listAllRules(tenantId).stream()
                .map(e -> { MerchantRechargeRuleVO vo = new MerchantRechargeRuleVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    @PutMapping("/recharge-rules")
    public Result<Void> replaceRechargeRules(@PathVariable Long tenantId,
                                             @Valid @RequestBody List<V1MerchantRechargeRuleDTO> rules) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        merchantRechargeRuleService.replaceRules(tenantId, rules);
        return Result.success();
    }
}
