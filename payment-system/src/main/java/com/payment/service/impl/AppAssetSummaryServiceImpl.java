package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.AppAssetActivityVO;
import com.payment.dto.AppTenantAssetSummaryVO;
import com.payment.dto.WalletLogVO;
import com.payment.entity.CouponExpireRecord;
import com.payment.entity.CouponLockRecord;
import com.payment.entity.CouponReceiveRecord;
import com.payment.entity.CouponReleaseRecord;
import com.payment.entity.CouponWriteOffRecord;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.MemberPointsLog;
import com.payment.entity.MemberGrowthLog;
import com.payment.entity.MerchantWalletLog;
import com.payment.entity.MerchantWalletAccount;
import com.payment.entity.Tenant;
import com.payment.entity.TenantMember;
import com.payment.entity.UnifiedWalletLog;
import com.payment.entity.UserCoupon;
import com.payment.enums.PointsDeductStatusEnum;
import com.payment.enums.UserCouponStatusEnum;
import com.payment.mapper.CouponExpireRecordMapper;
import com.payment.mapper.CouponLockRecordMapper;
import com.payment.mapper.CouponReceiveRecordMapper;
import com.payment.mapper.CouponReleaseRecordMapper;
import com.payment.mapper.CouponWriteOffRecordMapper;
import com.payment.mapper.MemberGrowthLogMapper;
import com.payment.mapper.MemberPointsAccountMapper;
import com.payment.mapper.MemberPointsLogMapper;
import com.payment.mapper.MerchantWalletAccountMapper;
import com.payment.mapper.MerchantWalletLogMapper;
import com.payment.mapper.TenantMapper;
import com.payment.mapper.TenantMemberMapper;
import com.payment.mapper.UnifiedWalletLogMapper;
import com.payment.mapper.UserCouponMapper;
import com.payment.service.AppAssetSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户端资产汇总服务实现。
 */
@Service
@RequiredArgsConstructor
public class AppAssetSummaryServiceImpl implements AppAssetSummaryService {

    private final TenantMemberMapper tenantMemberMapper;
    private final TenantMapper tenantMapper;
    private final MerchantWalletAccountMapper merchantWalletAccountMapper;
    private final MemberPointsAccountMapper memberPointsAccountMapper;
    private final MemberPointsLogMapper memberPointsLogMapper;
    private final MemberGrowthLogMapper memberGrowthLogMapper;
    private final UserCouponMapper userCouponMapper;
    private final UnifiedWalletLogMapper unifiedWalletLogMapper;
    private final MerchantWalletLogMapper merchantWalletLogMapper;
    private final CouponReceiveRecordMapper couponReceiveRecordMapper;
    private final CouponLockRecordMapper couponLockRecordMapper;
    private final CouponReleaseRecordMapper couponReleaseRecordMapper;
    private final CouponWriteOffRecordMapper couponWriteOffRecordMapper;
    private final CouponExpireRecordMapper couponExpireRecordMapper;

    @Override
    public List<AppTenantAssetSummaryVO> listTenantAssetSummaries(Long platformUserId) {
        List<TenantMember> members = tenantMemberMapper.selectList(new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getPlatformUserId, platformUserId));
        List<MerchantWalletAccount> wallets = merchantWalletAccountMapper.selectList(new LambdaQueryWrapper<MerchantWalletAccount>()
                .eq(MerchantWalletAccount::getPlatformUserId, platformUserId));
        List<MemberPointsAccount> pointsAccounts = memberPointsAccountMapper.selectList(new LambdaQueryWrapper<MemberPointsAccount>()
                .eq(MemberPointsAccount::getPlatformUserId, platformUserId));

        Set<Long> tenantIds = new LinkedHashSet<>();
        members.stream().map(TenantMember::getTenantId).filter(Objects::nonNull).forEach(tenantIds::add);
        wallets.stream().map(MerchantWalletAccount::getTenantId).filter(Objects::nonNull).forEach(tenantIds::add);
        pointsAccounts.stream().map(MemberPointsAccount::getTenantId).filter(Objects::nonNull).forEach(tenantIds::add);
        Map<Long, Map<String, Integer>> couponCounts = loadCouponCounts(platformUserId);
        Map<Long, Integer> expiringCouponCounts = loadExpiringCouponCounts(platformUserId, LocalDateTime.now());
        Map<Long, Integer> growthTotals = loadGrowthTotals(platformUserId);
        Map<Long, Integer> expiringPoints = loadExpiringPoints(platformUserId, LocalDateTime.now());
        tenantIds.addAll(couponCounts.keySet());
        tenantIds.addAll(expiringCouponCounts.keySet());
        tenantIds.addAll(growthTotals.keySet());
        tenantIds.addAll(expiringPoints.keySet());
        if (tenantIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Tenant> tenantMap = tenantMapper.selectBatchIds(tenantIds).stream()
                .filter(tenant -> tenant.getDeleted() == null || tenant.getDeleted() == 0)
                .collect(Collectors.toMap(Tenant::getId, Function.identity(), (left, right) -> left));
        Map<Long, TenantMember> memberMap = members.stream()
                .filter(member -> member.getTenantId() != null)
                .collect(Collectors.toMap(TenantMember::getTenantId, Function.identity(), (left, right) -> left));
        Map<Long, MerchantWalletAccount> walletMap = wallets.stream()
                .filter(wallet -> wallet.getTenantId() != null)
                .collect(Collectors.toMap(MerchantWalletAccount::getTenantId, Function.identity(), (left, right) -> left));
        Map<Long, MemberPointsAccount> pointsMap = pointsAccounts.stream()
                .filter(account -> account.getTenantId() != null)
                .collect(Collectors.toMap(MemberPointsAccount::getTenantId, Function.identity(), (left, right) -> left));

        return tenantIds.stream()
                .filter(tenantMap::containsKey)
                .sorted(Comparator.naturalOrder())
                .map(tenantId -> toSummary(tenantId, tenantMap, memberMap, walletMap, pointsMap,
                        couponCounts, expiringCouponCounts, growthTotals, expiringPoints))
                .toList();
    }

    private AppTenantAssetSummaryVO toSummary(Long tenantId,
                                              Map<Long, Tenant> tenantMap,
                                              Map<Long, TenantMember> memberMap,
                                              Map<Long, MerchantWalletAccount> walletMap,
                                              Map<Long, MemberPointsAccount> pointsMap,
                                              Map<Long, Map<String, Integer>> couponCounts,
                                              Map<Long, Integer> expiringCouponCounts,
                                              Map<Long, Integer> growthTotals,
                                              Map<Long, Integer> expiringPoints) {
        Tenant tenant = tenantMap.get(tenantId);
        TenantMember member = memberMap.get(tenantId);
        MerchantWalletAccount wallet = walletMap.get(tenantId);
        MemberPointsAccount points = pointsMap.get(tenantId);

        AppTenantAssetSummaryVO vo = new AppTenantAssetSummaryVO();
        vo.setTenantId(tenantId);
        vo.setTenantName(tenant.getName());
        vo.setMemberStatus(member == null ? null : member.getMemberStatus());
        vo.setWalletAvailableAmount(wallet == null || wallet.getAvailableAmount() == null ? BigDecimal.ZERO : wallet.getAvailableAmount());
        vo.setWalletFrozenAmount(wallet == null || wallet.getFrozenAmount() == null ? BigDecimal.ZERO : wallet.getFrozenAmount());
        int availablePoints = points == null || points.getPoints() == null ? 0 : Math.max(0, points.getPoints());
        vo.setPoints(availablePoints);
        vo.setExpiringSoonPoints(Math.min(availablePoints, expiringPoints.getOrDefault(tenantId, 0)));
        Map<String, Integer> statusCounts = couponCounts.getOrDefault(tenantId, Collections.emptyMap());
        vo.setUsableCouponCount(statusCounts.getOrDefault(UserCouponStatusEnum.RECEIVED.name(), 0)
                + statusCounts.getOrDefault(UserCouponStatusEnum.RELEASED.name(), 0));
        vo.setLockedCouponCount(statusCounts.getOrDefault(UserCouponStatusEnum.LOCKED.name(), 0));
        vo.setUsedCouponCount(statusCounts.getOrDefault(UserCouponStatusEnum.USED.name(), 0));
        vo.setExpiredCouponCount(statusCounts.getOrDefault(UserCouponStatusEnum.EXPIRED.name(), 0));
        vo.setExpiringSoonCouponCount(expiringCouponCounts.getOrDefault(tenantId, 0));
        vo.setTotalGrowth(Math.max(0, growthTotals.getOrDefault(tenantId, 0)));
        return vo;
    }

    private Map<Long, Map<String, Integer>> loadCouponCounts(Long platformUserId) {
        List<Map<String, Object>> rows = userCouponMapper.selectMaps(new QueryWrapper<UserCoupon>()
                .select("tenant_id AS tenantId", "coupon_status AS couponStatus", "COUNT(*) AS count")
                .eq("platform_user_id", platformUserId)
                .isNotNull("tenant_id")
                .groupBy("tenant_id", "coupon_status"));
        Map<Long, Map<String, Integer>> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long tenantId = longValue(row, "tenantId", "tenant_id");
            String status = stringValue(row, "couponStatus", "coupon_status");
            Integer count = intValue(row, "count");
            if (tenantId != null && status != null) {
                result.computeIfAbsent(tenantId, ignored -> new HashMap<>()).put(status, count);
            }
        }
        return result;
    }

    private Map<Long, Integer> loadExpiringCouponCounts(Long platformUserId, LocalDateTime now) {
        List<Map<String, Object>> rows = userCouponMapper.selectMaps(new QueryWrapper<UserCoupon>()
                .select("tenant_id AS tenantId", "COUNT(*) AS count")
                .eq("platform_user_id", platformUserId)
                .isNotNull("tenant_id")
                .in("coupon_status", List.of(UserCouponStatusEnum.RECEIVED.name(), UserCouponStatusEnum.RELEASED.name()))
                .isNotNull("expire_time")
                .ge("expire_time", now)
                .le("expire_time", now.plusDays(30))
                .groupBy("tenant_id"));
        return rowsToIntMap(rows, "tenantId", "tenant_id", "count");
    }

    private Map<Long, Integer> loadGrowthTotals(Long platformUserId) {
        List<Map<String, Object>> rows = memberGrowthLogMapper.selectMaps(new QueryWrapper<MemberGrowthLog>()
                .select("tenant_id AS tenantId", "IFNULL(SUM(change_growth), 0) AS totalGrowth")
                .eq("platform_user_id", platformUserId)
                .groupBy("tenant_id"));
        return rowsToIntMap(rows, "tenantId", "tenant_id", "totalGrowth", "total_growth");
    }

    private Map<Long, Integer> loadExpiringPoints(Long platformUserId, LocalDateTime now) {
        List<Map<String, Object>> rows = memberPointsLogMapper.selectMaps(new QueryWrapper<MemberPointsLog>()
                .select("tenant_id AS tenantId", "IFNULL(SUM(change_points), 0) AS points")
                .eq("platform_user_id", platformUserId)
                .gt("change_points", 0)
                .eq("status", PointsDeductStatusEnum.CONFIRMED.name())
                .isNotNull("expire_time")
                .ge("expire_time", now)
                .le("expire_time", now.plusDays(30))
                .groupBy("tenant_id"));
        return rowsToIntMap(rows, "tenantId", "tenant_id", "points");
    }

    @Override
    public List<AppAssetActivityVO> listAssetActivities(Long platformUserId, Integer size) {
        int limit = size == null ? 20 : Math.max(1, Math.min(size, 50));
        List<AppAssetActivityVO> activities = new ArrayList<>();

        unifiedWalletLogMapper.selectPage(new Page<>(1, limit, false), new LambdaQueryWrapper<UnifiedWalletLog>()
                        .eq(UnifiedWalletLog::getPlatformUserId, platformUserId)
                        .orderByDesc(UnifiedWalletLog::getCreateTime))
                .getRecords()
                .forEach(log -> activities.add(fromUnifiedWallet(log)));
        merchantWalletLogMapper.selectPage(new Page<>(1, limit, false), new LambdaQueryWrapper<MerchantWalletLog>()
                        .eq(MerchantWalletLog::getPlatformUserId, platformUserId)
                        .orderByDesc(MerchantWalletLog::getCreateTime))
                .getRecords()
                .forEach(log -> activities.add(fromMerchantWallet(log)));
        memberPointsLogMapper.selectPage(new Page<>(1, limit, false), new LambdaQueryWrapper<MemberPointsLog>()
                .eq(MemberPointsLog::getPlatformUserId, platformUserId)
                        .orderByDesc(MemberPointsLog::getCreateTime))
                .getRecords()
                .stream()
                .map(this::fromPoints)
                .filter(Objects::nonNull)
                .forEach(activities::add);
        memberGrowthLogMapper.selectPage(new Page<>(1, limit, false), new LambdaQueryWrapper<MemberGrowthLog>()
                        .eq(MemberGrowthLog::getPlatformUserId, platformUserId)
                        .orderByDesc(MemberGrowthLog::getCreateTime))
                .getRecords()
                .forEach(log -> activities.add(fromGrowth(log)));
        appendCouponActivities(platformUserId, limit, activities);

        Map<Long, String> tenantNames = loadTenantNames(activities);
        activities.forEach(activity -> {
            if (activity.getTenantId() != null) {
                activity.setTenantName(tenantNames.get(activity.getTenantId()));
            }
        });

        return activities.stream()
                .filter(activity -> activity.getOccurredAt() != null)
                .sorted(Comparator.comparing(AppAssetActivityVO::getOccurredAt).reversed())
                .limit(limit)
                .toList();
    }

    private AppAssetActivityVO fromUnifiedWallet(UnifiedWalletLog log) {
        WalletLogVO walletLog = new WalletLogVO();
        walletLog.setWalletType("UNIFIED");
        walletLog.setBizType(log.getBizType());
        walletLog.setBizNo(log.getBizNo());
        walletLog.setChangeAmount(log.getChangeAmount());
        walletLog.setBalanceBefore(log.getBalanceBefore());
        walletLog.setBalanceAfter(log.getBalanceAfter());
        walletLog.setRemark(log.getRemark());
        walletLog.setCreateTime(log.getCreateTime());
        walletLog.attachTrace();
        return activity("WALLET", walletLog.getTrace().getTitle(), walletLog.getTrace().getSource(),
                log.getCreateTime(), null, log.getBizNo(), moneyText(log.getChangeAmount()), walletLog.getTrace().getTone(), "/history");
    }

    private AppAssetActivityVO fromMerchantWallet(MerchantWalletLog log) {
        WalletLogVO walletLog = new WalletLogVO();
        walletLog.setWalletType("MERCHANT");
        walletLog.setTenantId(log.getTenantId());
        walletLog.setBizType(log.getBizType());
        walletLog.setBizNo(log.getBizNo());
        walletLog.setChangeAmount(log.getChangeAmount());
        walletLog.setBalanceBefore(log.getBalanceBefore());
        walletLog.setBalanceAfter(log.getBalanceAfter());
        walletLog.setRemark(log.getRemark());
        walletLog.setCreateTime(log.getCreateTime());
        walletLog.attachTrace();
        return activity("WALLET", walletLog.getTrace().getTitle(), walletLog.getTrace().getSource(),
                log.getCreateTime(), log.getTenantId(), log.getBizNo(), moneyText(log.getChangeAmount()), walletLog.getTrace().getTone(),
                log.getTenantId() == null ? "/history" : "/wallet/tenants/" + log.getTenantId());
    }

    private AppAssetActivityVO fromPoints(MemberPointsLog log) {
        String status = log.getStatus();
        if (PointsDeductStatusEnum.EXPIRED.name().equals(status)
                && log.getChangePoints() != null
                && log.getChangePoints() > 0) {
            return null;
        }
        if (PointsDeductStatusEnum.RELEASED.name().equals(status)) {
            int points = Math.abs(log.getChangePoints() == null ? 0 : log.getChangePoints());
            return activity("POINTS", "积分已释放", trimToFallback(log.getReleaseReason(), "订单未完成，预扣积分已退回"),
                    log.getReleaseTime() == null ? log.getCreateTime() : log.getReleaseTime(),
                    log.getTenantId(), log.getBizNo(), "+" + points + " 分", "positive",
                    log.getTenantId() == null ? "/wallet" : "/points/" + log.getTenantId());
        }
        boolean preHold = PointsDeductStatusEnum.PRE_HOLD.name().equals(status);
        String title = log.getChangePoints() == null || log.getChangePoints() >= 0 ? "积分入账" : "积分扣减";
        if (preHold) {
            title = "积分已预扣";
        }
        String amount = log.getChangePoints() == null ? null : (log.getChangePoints() > 0 ? "+" : "") + log.getChangePoints() + " 分";
        return activity("POINTS", title, trimToFallback(log.getRemark(), trimToFallback(log.getBizType(), "积分变动")),
                log.getConfirmTime() == null ? log.getCreateTime() : log.getConfirmTime(),
                log.getTenantId(), log.getBizNo(), amount,
                preHold ? "neutral" : log.getChangePoints() != null && log.getChangePoints() < 0 ? "negative" : "positive",
                log.getTenantId() == null ? "/wallet" : "/points/" + log.getTenantId());
    }

    private AppAssetActivityVO fromGrowth(MemberGrowthLog log) {
        String title = log.getChangeGrowth() == null || log.getChangeGrowth() >= 0 ? "成长值增加" : "成长值扣减";
        String amount = log.getChangeGrowth() == null ? null : (log.getChangeGrowth() > 0 ? "+" : "") + log.getChangeGrowth();
        return activity("GROWTH", title, trimToFallback(log.getRemark(), trimToFallback(log.getBizType(), "成长值变动")),
                log.getCreateTime(), log.getTenantId(), log.getBizNo(), amount,
                log.getChangeGrowth() != null && log.getChangeGrowth() < 0 ? "negative" : "positive",
                log.getTenantId() == null ? "/wallet" : "/growth/" + log.getTenantId());
    }

    private void appendCouponActivities(Long platformUserId, int limit, List<AppAssetActivityVO> activities) {
        Set<Long> userCouponIds = userCouponMapper.selectList(new QueryWrapper<UserCoupon>()
                        .select("id")
                        .eq("platform_user_id", platformUserId))
                .stream()
                .map(UserCoupon::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        couponReceiveRecordMapper.selectPage(new Page<>(1, limit, false), new LambdaQueryWrapper<CouponReceiveRecord>()
                        .eq(CouponReceiveRecord::getPlatformUserId, platformUserId)
                        .orderByDesc(CouponReceiveRecord::getReceiveTime))
                .getRecords()
                .forEach(record -> activities.add(activity("COUPON", "优惠券领取", "优惠券已进入账户",
                        record.getReceiveTime(), record.getTenantId(), record.getBizNo(), null, "positive",
                        couponPath(record.getTenantId(), "my"))));
        if (userCouponIds.isEmpty()) {
            return;
        }
        couponLockRecordMapper.selectPage(new Page<>(1, limit, false), new LambdaQueryWrapper<CouponLockRecord>()
                        .in(CouponLockRecord::getUserCouponId, userCouponIds)
                        .orderByDesc(CouponLockRecord::getLockTime))
                .getRecords()
                .forEach(record -> activities.add(activity("COUPON", "优惠券锁定", "订单 " + record.getOrderNo() + " 锁定优惠券",
                        record.getLockTime(), record.getTenantId(), record.getOrderNo(), null, "neutral", couponPath(record.getTenantId(), "my"))));
        couponReleaseRecordMapper.selectPage(new Page<>(1, limit, false), new LambdaQueryWrapper<CouponReleaseRecord>()
                        .in(CouponReleaseRecord::getUserCouponId, userCouponIds)
                        .orderByDesc(CouponReleaseRecord::getReleaseTime))
                .getRecords()
                .forEach(record -> activities.add(activity("COUPON", "优惠券释放", trimToFallback(record.getReleaseReason(), "优惠券已释放"),
                        record.getReleaseTime(), record.getTenantId(), record.getOrderNo(), null, "neutral", couponPath(record.getTenantId(), "my"))));
        couponWriteOffRecordMapper.selectPage(new Page<>(1, limit, false), new LambdaQueryWrapper<CouponWriteOffRecord>()
                        .in(CouponWriteOffRecord::getUserCouponId, userCouponIds)
                        .orderByDesc(CouponWriteOffRecord::getWriteOffTime))
                .getRecords()
                .forEach(record -> activities.add(activity("COUPON", "优惠券核销", "订单 " + record.getOrderNo() + " 已使用优惠券",
                        record.getWriteOffTime(), record.getTenantId(), record.getOrderNo(), moneyText(record.getDiscountAmount()), "positive", couponPath(record.getTenantId(), "expired"))));
        couponExpireRecordMapper.selectPage(new Page<>(1, limit, false), new LambdaQueryWrapper<CouponExpireRecord>()
                        .eq(CouponExpireRecord::getPlatformUserId, platformUserId)
                        .orderByDesc(CouponExpireRecord::getExpireTime))
                .getRecords()
                .forEach(record -> activities.add(activity("COUPON", "优惠券过期", trimToFallback(record.getExpireReason(), "优惠券已失效"),
                        record.getExpireTime(), record.getTenantId(), record.getBizNo(), null, "negative", couponPath(record.getTenantId(), "expired"))));
    }

    private Map<Long, String> loadTenantNames(List<AppAssetActivityVO> activities) {
        Set<Long> tenantIds = activities.stream()
                .map(AppAssetActivityVO::getTenantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (tenantIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return tenantMapper.selectBatchIds(tenantIds).stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName, (left, right) -> left));
    }

    private AppAssetActivityVO activity(String assetType,
                                        String title,
                                        String description,
                                        LocalDateTime occurredAt,
                                        Long tenantId,
                                        String bizNo,
                                        String amountText,
                                        String tone,
                                        String actionPath) {
        AppAssetActivityVO vo = new AppAssetActivityVO();
        vo.setAssetType(assetType);
        vo.setTitle(title);
        vo.setDescription(description);
        vo.setOccurredAt(occurredAt);
        vo.setTenantId(tenantId);
        vo.setBizNo(bizNo);
        vo.setAmountText(amountText);
        vo.setTone(tone);
        vo.setActionPath(actionPath);
        return vo;
    }

    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return (amount.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + "¥" + amount.stripTrailingZeros().toPlainString();
    }

    private String couponPath(Long tenantId, String tab) {
        String path = "/coupons?tab=" + tab;
        return tenantId == null ? path : path + "&tenantId=" + tenantId;
    }

    private String trimToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Map<Long, Integer> rowsToIntMap(List<Map<String, Object>> rows, String tenantCamelKey, String tenantSnakeKey, String... valueKeys) {
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long tenantId = longValue(row, tenantCamelKey, tenantSnakeKey);
            Integer value = intValue(row, valueKeys);
            if (tenantId != null) {
                result.put(tenantId, value);
            }
        }
        return result;
    }

    private Long longValue(Map<String, Object> row, String... keys) {
        Object value = value(row, keys);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return null;
    }

    private Integer intValue(Map<String, Object> row, String... keys) {
        Object value = value(row, keys);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return new BigDecimal(text).intValue();
        }
        return 0;
    }

    private String stringValue(Map<String, Object> row, String... keys) {
        Object value = value(row, keys);
        return value == null ? null : String.valueOf(value);
    }

    private Object value(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key)) {
                return row.get(key);
            }
            String upper = key.toUpperCase();
            if (row.containsKey(upper)) {
                return row.get(upper);
            }
        }
        return null;
    }
}
