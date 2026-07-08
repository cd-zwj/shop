package com.payment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.constant.MerchantPermission;
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

/**
 * 商户端财务与积分控制器（Merchant 端）。
 * <p>提供商户钱包余额查询、积分规则管理、充值规则管理以及收支流水查询等功能。
 * 流水数据合并自商户钱包（merchant_wallet_log）和统一钱包（unified_wallet_log）两个数据源，
 * 并通过 bizNo 匹配本商户订单来避免跨商户数据污染。</p>
 */
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

    /**
     * 查询商户钱包余额汇总。
     *
     * @param tenantId 租户 ID
     * @return 钱包余额汇总信息（可用余额、冻结金额、总收入、总提现）
     */
    @GetMapping("/wallet-summary")
    public Result<V1MerchantBalanceVO> getWalletSummary(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        requireFinancePermission(tenantId);
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

    /**
     * 查询当前租户的积分规则。
     *
     * @param tenantId 租户 ID
     * @return 积分规则信息（积分比例、是否启用）
     */
    @GetMapping("/points-rule")
    public Result<V1MerchantPointsRuleDTO> getPointsRule(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        requireRulePermission(tenantId);
        PointsRule rule = pointsRuleMapper.selectOne(new LambdaQueryWrapper<PointsRule>()
                .eq(PointsRule::getTenantId, tenantId)
                .eq(PointsRule::getStatus, 1));
        V1MerchantPointsRuleDTO dto = new V1MerchantPointsRuleDTO();
        dto.setPointsRatio(rule == null ? 0 : rule.getPointsAmount());
        dto.setEnabled(rule != null && rule.getStatus() != null && rule.getStatus() == 1);
        return Result.success(dto);
    }

    /**
     * 更新积分规则（不存在则新建）。
     *
     * @param tenantId 租户 ID
     * @param dto      积分规则参数（积分比例、是否启用）
     * @return 操作结果
     */
    @PutMapping("/points-rule")
    public Result<Void> updatePointsRule(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @Valid @RequestBody V1MerchantPointsRuleDTO dto) {
        requireRulePermission(tenantId);
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

    /**
     * 查询当前租户的所有充值规则。
     *
     * @param tenantId 租户 ID
     * @return 充值规则列表
     */
    @GetMapping("/recharge-rules")
    public Result<List<MerchantRechargeRuleVO>> listRechargeRules(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        requireFinancePermission(tenantId);
        return Result.success(merchantRechargeRuleService.listAllRules(tenantId).stream()
                .map(e -> { MerchantRechargeRuleVO vo = new MerchantRechargeRuleVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    /**
     * 全量替换充值规则列表（先删后插）。
     *
     * @param tenantId 租户 ID
     * @param rules    新的充值规则列表
     * @return 操作结果
     */
    @PutMapping("/recharge-rules")
    public Result<Void> replaceRechargeRules(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                             @Valid @RequestBody List<V1MerchantRechargeRuleDTO> rules) {
        requireFinancePermission(tenantId);
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
    public Result<PageResult<MerchantTransactionVO>> listTransactions(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {

        requireFinancePermission(tenantId);

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
        int safeCurrent = current == null || current < 1 ? 1 : current;
        int safeSize = size == null || size < 1 ? 20 : size;
        int total = all.size();
        int from = Math.min((safeCurrent - 1) * safeSize, total);
        int to = Math.min(from + safeSize, total);

        return Result.success(new PageResult<>(all.subList(from, to), total, safeCurrent, safeSize));
    }

    /* ---------- private helpers ---------- */

    private void requireFinancePermission(Long tenantId) {
        v1MerchantSupportService.requirePermission(tenantId, PlatformSessionHelper.getPlatformUserId(), MerchantPermission.FINANCE_VIEW);
    }

    private void requireRulePermission(Long tenantId) {
        v1MerchantSupportService.requirePermission(tenantId, PlatformSessionHelper.getPlatformUserId(), MerchantPermission.RULE_MANAGE);
    }

    /**
     * 查询商户钱包流水记录。
     * <p>
     * 从 merchant_wallet_log 表按租户 ID、业务类型和日期范围查询，自动租户隔离。
     * </p>
     *
     * @param tenantId  租户 ID
     * @param type      业务类型筛选（可选）
     * @param startDate 开始日期（可选，格式 yyyy-MM-dd）
     * @param endDate   结束日期（可选，格式 yyyy-MM-dd）
     * @return 商户钱包交易记录列表
     */
    private List<MerchantTransactionVO> queryMerchantLogs(Long tenantId, String type, String startDate, String endDate) {
        LambdaQueryWrapper<MerchantWalletLog> w = new LambdaQueryWrapper<MerchantWalletLog>()
                .eq(MerchantWalletLog::getTenantId, tenantId);
        applyCommonFilters(w, type, startDate, endDate, MerchantWalletLog::getBizType, MerchantWalletLog::getCreateTime);
        w.orderByDesc(MerchantWalletLog::getCreateTime);
        return merchantWalletLogMapper.selectList(w).stream()
                .map(this::toMerchantVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询统一钱包中与本商户订单关联的流水记录。
     * <p>
     * 从 unified_wallet_log 表查询属于本租户会员的流水，并通过 inSql 子查询
     * 限定 bizNo 必须匹配本商户的 sales_order.order_no，避免跨商户数据污染。
     * </p>
     *
     * @param userIds   本租户下的所有 platformUserId 列表
     * @param tenantId  租户 ID，用于 bizNo 子查询过滤
     * @param type      业务类型筛选（可选）
     * @param startDate 开始日期（可选，格式 yyyy-MM-dd）
     * @param endDate   结束日期（可选，格式 yyyy-MM-dd）
     * @return 统一钱包交易记录列表
     */
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
     * <p>
     * 使用泛型 SFunction 兼容不同实体类型（MerchantWalletLog / UnifiedWalletLog），
     * 按业务类型精确匹配和创建时间区间过滤。
     * </p>
     *
     * @param <T>           实体类型
     * @param w             MyBatis-Plus 查询条件包装器
     * @param type          业务类型（可选，转大写后匹配）
     * @param startDate     开始日期（可选，格式 yyyy-MM-dd，含当天）
     * @param endDate       结束日期（可选，格式 yyyy-MM-dd，不含当天）
     * @param bizTypeCol    业务类型字段引用
     * @param createTimeCol 创建时间字段引用
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

    /**
     * 将商户钱包日志转换为交易 VO。
     *
     * @param log 商户钱包流水记录实体
     * @return 交易 VO 对象
     */
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

    /**
     * 将统一钱包日志转换为交易 VO。
     *
     * @param log 统一钱包流水记录实体
     * @return 交易 VO 对象
     */
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
