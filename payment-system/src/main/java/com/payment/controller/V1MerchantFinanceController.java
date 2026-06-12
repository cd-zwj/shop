package com.payment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.Result;
import com.payment.dto.MerchantRechargeRuleVO;
import com.payment.dto.MerchantTransactionVO;
import com.payment.dto.V1MerchantBalanceVO;
import com.payment.dto.V1MerchantPointsRuleDTO;
import com.payment.dto.V1MerchantRechargeRuleDTO;
import com.payment.entity.MerchantBalance;
import com.payment.entity.MerchantRechargeRule;
import com.payment.entity.MerchantWalletLog;
import com.payment.entity.PointsRule;
import com.payment.entity.TenantMember;
import com.payment.entity.UnifiedWalletLog;
import com.payment.mapper.MerchantBalanceMapper;
import com.payment.mapper.MerchantWalletLogMapper;
import com.payment.mapper.PointsRuleMapper;
import com.payment.mapper.TenantMemberMapper;
import com.payment.mapper.UnifiedWalletLogMapper;
import com.payment.service.MerchantRechargeRuleService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}")
@RequiredArgsConstructor
public class V1MerchantFinanceController {

    private final V1MerchantSupportService v1MerchantSupportService;
    private final MerchantBalanceMapper merchantBalanceMapper;
    private final MerchantWalletLogMapper merchantWalletLogMapper;
    private final UnifiedWalletLogMapper unifiedWalletLogMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final PointsRuleMapper pointsRuleMapper;
    private final MerchantRechargeRuleService merchantRechargeRuleService;

    @GetMapping("/wallet-summary")
    public Result<V1MerchantBalanceVO> getWalletSummary(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
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
    public Result<V1MerchantPointsRuleDTO> getPointsRule(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        PointsRule rule = pointsRuleMapper.selectOne(new LambdaQueryWrapper<PointsRule>()
                .eq(PointsRule::getTenantId, tenantId)
                .eq(PointsRule::getStatus, 1));
        V1MerchantPointsRuleDTO dto = new V1MerchantPointsRuleDTO();
        dto.setPointsRatio(rule == null ? 0 : rule.getPointsAmount());
        dto.setEnabled(rule != null && rule.getStatus() != null && rule.getStatus() == 1);
        return Result.success(dto);
    }

    @PutMapping("/points-rule")
    public Result<Void> updatePointsRule(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @Valid @RequestBody V1MerchantPointsRuleDTO dto) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        PointsRule rule = pointsRuleMapper.selectOne(new LambdaQueryWrapper<PointsRule>()
                .eq(PointsRule::getTenantId, tenantId)
                .eq(PointsRule::getStatus, 1));
        if (rule == null) {
            rule = new PointsRule();
            rule.setTenantId(tenantId);
            rule.setCreateTime(LocalDateTime.now());
        }
        rule.setPointsAmount(dto.getPointsRatio());
        rule.setStatus(Boolean.TRUE.equals(dto.getEnabled()) ? 1 : 0);
        rule.setUpdateTime(LocalDateTime.now());
        if (rule.getId() == null) {
            pointsRuleMapper.insert(rule);
        } else {
            pointsRuleMapper.updateById(rule);
        }
        return Result.success();
    }

    @GetMapping("/recharge-rules")
    public Result<List<MerchantRechargeRuleVO>> listRechargeRules(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        return Result.success(merchantRechargeRuleService.listAllRules(tenantId).stream()
                .map(e -> { MerchantRechargeRuleVO vo = new MerchantRechargeRuleVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    @PutMapping("/recharge-rules")
    public Result<Void> replaceRechargeRules(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                             @Valid @RequestBody List<V1MerchantRechargeRuleDTO> rules) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        merchantRechargeRuleService.replaceRules(tenantId, rules);
        return Result.success();
    }

    /**
     * 商户收支流水列表。
     * 数据源：merchant_wallet_log（商户钱包）+ unified_wallet_log（统一钱包）。
     * unified_wallet_log 仅纳入 bizNo 匹配本商户 sales_order 的记录，避免跨商户数据污染。
     * 全量查询后合并排序，手动分页以保证 total 准确。
     */
    @GetMapping("/transactions")
    public Result<Page<MerchantTransactionVO>> listTransactions(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {

        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());

        // 1. merchant_wallet_log（有 tenant_id，自动租户隔离，无 LIMIT 保证 total 准确）
        List<MerchantTransactionVO> all = new ArrayList<>();
        all.addAll(queryMerchantLogs(tenantId, type, startDate, endDate));

        // 2. unified_wallet_log
        //    口径限定：bizNo 必须匹配本商户的 sales_order.order_no，排除其他商户/平台场景的流水
        List<Long> userIds = tenantMemberMapper.selectList(
                new LambdaQueryWrapper<TenantMember>()
                        .eq(TenantMember::getTenantId, tenantId)
                        .select(TenantMember::getPlatformUserId)
        ).stream().map(TenantMember::getPlatformUserId).collect(Collectors.toList());

        if (!userIds.isEmpty()) {
            all.addAll(queryUnifiedLogs(userIds, tenantId, type, startDate, endDate));
        }

        // 3. 按时间倒序排序
        all.sort((a, b) -> {
            String ta = a.getCreateTime() != null ? a.getCreateTime() : "";
            String tb = b.getCreateTime() != null ? b.getCreateTime() : "";
            return tb.compareTo(ta);
        });

        // 4. 手动分页（total = 合并后真实总数）
        int total = all.size();
        int from = Math.min((current - 1) * size, total);
        int to = Math.min(from + size, total);

        Page<MerchantTransactionVO> voPage = new Page<>(current, size, total);
        voPage.setRecords(all.subList(from, to));
        return Result.success(voPage);
    }

    /* ---------- private helpers ---------- */

    private List<MerchantTransactionVO> queryMerchantLogs(Long tenantId, String type, String startDate, String endDate) {
        LambdaQueryWrapper<MerchantWalletLog> w = new LambdaQueryWrapper<MerchantWalletLog>()
                .eq(MerchantWalletLog::getTenantId, tenantId);
        applyCommonFilters(w, type, startDate, endDate, MerchantWalletLog::getBizType, MerchantWalletLog::getCreateTime);
        w.orderByDesc(MerchantWalletLog::getCreateTime);
        return merchantWalletLogMapper.selectList(w).stream()
                .map(this::toMerchantVO)
                .collect(Collectors.toList());
    }

    private List<MerchantTransactionVO> queryUnifiedLogs(List<Long> userIds, Long tenantId,
                                                          String type, String startDate, String endDate) {
        LambdaQueryWrapper<UnifiedWalletLog> w = new LambdaQueryWrapper<UnifiedWalletLog>()
                .in(UnifiedWalletLog::getPlatformUserId, userIds)
                // 只取 bizNo 匹配本商户订单的记录，防止跨商户数据污染
                .inSql(UnifiedWalletLog::getBizNo,
                        "SELECT order_no FROM sales_order WHERE tenant_id = " + tenantId + " AND deleted = 0");
        applyCommonFilters(w, type, startDate, endDate, UnifiedWalletLog::getBizType, UnifiedWalletLog::getCreateTime);
        w.orderByDesc(UnifiedWalletLog::getCreateTime);
        return unifiedWalletLogMapper.selectList(w).stream()
                .map(this::toUnifiedVO)
                .collect(Collectors.toList());
    }

    /**
     * 通用筛选条件：bizType + 日期范围。
     * 使用泛型 SFunction 兼容不同实体类型。
     */
    private <T> void applyCommonFilters(LambdaQueryWrapper<T> w,
                                        String type, String startDate, String endDate,
                                        com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, String> bizTypeCol,
                                        com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, LocalDateTime> createTimeCol) {
        if (type != null && !type.isBlank()) {
            w.eq(bizTypeCol, type.toUpperCase());
        }
        if (startDate != null && !startDate.isBlank()) {
            w.ge(createTimeCol, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isBlank()) {
            w.lt(createTimeCol, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }
    }

    private MerchantTransactionVO toMerchantVO(MerchantWalletLog log) {
        MerchantTransactionVO vo = new MerchantTransactionVO();
        vo.setId(log.getId());
        vo.setBizType(log.getBizType());
        vo.setBizNo(log.getBizNo());
        vo.setChangeAmount(log.getChangeAmount());
        vo.setBalanceBefore(log.getBalanceBefore());
        vo.setBalanceAfter(log.getBalanceAfter());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime() != null ? log.getCreateTime().toString() : null);
        return vo;
    }

    private MerchantTransactionVO toUnifiedVO(UnifiedWalletLog log) {
        MerchantTransactionVO vo = new MerchantTransactionVO();
        vo.setId(log.getId());
        vo.setBizType(log.getBizType());
        vo.setBizNo(log.getBizNo());
        vo.setChangeAmount(log.getChangeAmount());
        vo.setBalanceBefore(log.getBalanceBefore());
        vo.setBalanceAfter(log.getBalanceAfter());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime() != null ? log.getCreateTime().toString() : null);
        return vo;
    }
}
