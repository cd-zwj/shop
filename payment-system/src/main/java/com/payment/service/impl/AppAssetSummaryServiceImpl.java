package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.common.ResultCode;
import com.payment.dto.AppAssetActivityVO;
import com.payment.dto.AppTenantAssetSummaryVO;
import com.payment.dto.AssetActivityPageVO;
import com.payment.dto.AssetActivityQueryDTO;
import com.payment.dto.AssetHoldVO;
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
import com.payment.entity.SalesOrder;
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
import com.payment.mapper.SalesOrderMapper;
import com.payment.mapper.TenantMapper;
import com.payment.mapper.TenantMemberMapper;
import com.payment.mapper.UnifiedWalletLogMapper;
import com.payment.mapper.UserCouponMapper;
import com.payment.service.AppAssetSummaryService;
import com.payment.util.ActivityCursorUtil;
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

    private static final Set<String> ASSET_TYPES = Set.of("WALLET", "POINTS", "GROWTH", "COUPON");
    private static final int HOLD_LIMIT = 100;
    private static final Comparator<AppAssetActivityVO> ACTIVITY_ORDER = Comparator
            .comparing(AppAssetActivityVO::getOccurredAt, Comparator.reverseOrder())
            .thenComparing(AppAssetActivityVO::getSourceType)
            .thenComparing(AppAssetActivityVO::getSourceId, Comparator.nullsLast(Comparator.reverseOrder()));

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
    private final SalesOrderMapper salesOrderMapper;

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
        AssetActivityQueryDTO query = new AssetActivityQueryDTO();
        query.setSize(size == null ? 20 : Math.max(1, Math.min(size, 50)));
        return listAssetActivities(platformUserId, query).getRecords();
    }

    @Override
    public AssetActivityPageVO listAssetActivities(Long platformUserId, AssetActivityQueryDTO query) {
        ActivityQuery activityQuery = normalizeQuery(query);
        int candidateLimit = activityQuery.size() + 1;
        List<AppAssetActivityVO> activities = new ArrayList<>();

        if (activityQuery.includes("WALLET")) {
            appendWalletActivities(platformUserId, activityQuery, candidateLimit, activities);
        }
        if (activityQuery.includes("POINTS")) {
            appendPointsActivities(platformUserId, activityQuery, candidateLimit, activities);
        }
        if (activityQuery.includes("GROWTH")) {
            appendGrowthActivities(platformUserId, activityQuery, candidateLimit, activities);
        }
        if (activityQuery.includes("COUPON")) {
            appendCouponActivities(platformUserId, activityQuery, candidateLimit, activities);
        }

        List<AppAssetActivityVO> ordered = activities.stream()
                .filter(activity -> activity.getOccurredAt() != null)
                .filter(activity -> matchesQuery(activity, activityQuery))
                .sorted(ACTIVITY_ORDER)
                .toList();
        Map<Long, String> tenantNames = loadTenantNames(ordered);
        ordered.forEach(activity -> {
            if (activity.getTenantId() != null) {
                activity.setTenantName(tenantNames.get(activity.getTenantId()));
            }
        });

        boolean hasMore = ordered.size() > activityQuery.size();
        List<AppAssetActivityVO> records = ordered.stream().limit(activityQuery.size()).toList();
        String nextCursor = hasMore
                ? ActivityCursorUtil.encode(last(records).getOccurredAt(), last(records).getSourceType(), last(records).getSourceId())
                : null;
        return new AssetActivityPageVO(records, nextCursor, hasMore);
    }

    @Override
    public List<AssetHoldVO> listAssetHolds(Long platformUserId, Long tenantId) {
        if (tenantId != null && tenantId <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "商户ID必须大于0");
        }
        List<AssetHoldVO> holds = new ArrayList<>();

        List<UserCoupon> lockedCoupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getPlatformUserId, platformUserId)
                .eq(tenantId != null, UserCoupon::getTenantId, tenantId)
                .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.LOCKED.name())
                .orderByDesc(UserCoupon::getLockTime)
                .last("LIMIT " + HOLD_LIMIT));
        Map<Long, CouponLockRecord> activeLocks = loadActiveCouponLocks(lockedCoupons);
        lockedCoupons.forEach(coupon -> {
            CouponLockRecord lockRecord = activeLocks.get(coupon.getId());
            String orderNo = lockRecord == null ? coupon.getOrderNo() : lockRecord.getOrderNo();
            LocalDateTime occurredAt = lockRecord != null && lockRecord.getLockTime() != null
                    ? lockRecord.getLockTime() : coupon.getLockTime();
            holds.add(assetHold(coupon.getTenantId(), "COUPON", "LOCKED", "1 张优惠券", "订单待支付",
                    "SALES_ORDER", orderNo, occurredAt, orderActionPath("SALES_ORDER", orderNo)));
        });

        memberPointsLogMapper.selectList(new LambdaQueryWrapper<MemberPointsLog>()
                        .eq(MemberPointsLog::getPlatformUserId, platformUserId)
                        .eq(tenantId != null, MemberPointsLog::getTenantId, tenantId)
                        .eq(MemberPointsLog::getStatus, PointsDeductStatusEnum.PRE_HOLD.name())
                        .orderByDesc(MemberPointsLog::getCreateTime)
                        .last("LIMIT " + HOLD_LIMIT))
                .stream()
                .filter(log -> log.getChangePoints() != null && log.getChangePoints() != 0)
                .forEach(log -> {
                    String reason = "SALES_ORDER".equals(log.getBizType()) ? "订单待支付" : "积分预占";
                    holds.add(assetHold(log.getTenantId(), "POINTS", "PRE_HOLD",
                            "-" + Math.abs(log.getChangePoints()) + " 积分", reason,
                            log.getBizType(), log.getBizNo(), log.getCreateTime(), orderActionPath(log.getBizType(), log.getBizNo())));
                });

        merchantWalletAccountMapper.selectList(new LambdaQueryWrapper<MerchantWalletAccount>()
                        .eq(MerchantWalletAccount::getPlatformUserId, platformUserId)
                        .eq(tenantId != null, MerchantWalletAccount::getTenantId, tenantId)
                        .gt(MerchantWalletAccount::getFrozenAmount, BigDecimal.ZERO)
                        .orderByDesc(MerchantWalletAccount::getUpdateTime)
                        .last("LIMIT " + HOLD_LIMIT))
                .stream()
                .filter(wallet -> wallet.getFrozenAmount() != null
                        && wallet.getFrozenAmount().compareTo(BigDecimal.ZERO) > 0)
                .forEach(wallet -> holds.add(assetHold(wallet.getTenantId(), "WALLET", "FROZEN",
                        absoluteMoneyText(wallet.getFrozenAmount()), "冻结金额", null, null,
                        wallet.getUpdateTime(), null)));

        List<AssetHoldVO> orderedHolds = holds.stream()
                .sorted(Comparator.comparing(AssetHoldVO::getOccurredAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AssetHoldVO::getAssetType))
                .limit(HOLD_LIMIT)
                .toList();
        attachHoldTenantNames(orderedHolds);
        attachOwnedOrderActions(platformUserId, tenantId, orderedHolds);
        return orderedHolds;
    }

    private void attachHoldTenantNames(List<AssetHoldVO> holds) {
        Set<Long> tenantIds = holds.stream()
                .map(AssetHoldVO::getTenantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (tenantIds.isEmpty()) {
            return;
        }
        Map<Long, String> tenantNames = tenantMapper.selectBatchIds(tenantIds).stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName, (left, right) -> left));
        holds.forEach(hold -> hold.setTenantName(tenantNames.get(hold.getTenantId())));
    }

    private Map<Long, CouponLockRecord> loadActiveCouponLocks(List<UserCoupon> lockedCoupons) {
        Set<Long> userCouponIds = lockedCoupons.stream()
                .map(UserCoupon::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (userCouponIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return couponLockRecordMapper.selectList(new LambdaQueryWrapper<CouponLockRecord>()
                        .in(CouponLockRecord::getUserCouponId, userCouponIds)
                        .eq(CouponLockRecord::getLockStatus, UserCouponStatusEnum.LOCKED.name())
                        .orderByDesc(CouponLockRecord::getLockTime))
                .stream()
                .filter(record -> record.getUserCouponId() != null)
                .collect(Collectors.toMap(CouponLockRecord::getUserCouponId, Function.identity(), (left, right) -> left));
    }

    private AssetHoldVO assetHold(Long tenantId,
                                  String assetType,
                                  String holdStatus,
                                  String amountText,
                                  String reason,
                                  String bizType,
                                  String bizNo,
                                  LocalDateTime occurredAt,
                                  String actionPath) {
        AssetHoldVO hold = new AssetHoldVO();
        hold.setTenantId(tenantId);
        hold.setAssetType(assetType);
        hold.setHoldStatus(holdStatus);
        hold.setAmountText(amountText);
        hold.setReason(reason);
        hold.setBizType(bizType);
        hold.setBizNo(bizNo);
        hold.setOccurredAt(occurredAt);
        hold.setActionPath(actionPath);
        return hold;
    }

    private String orderActionPath(String bizType, String bizNo) {
        if (!"SALES_ORDER".equals(bizType) || bizNo == null || bizNo.isBlank()) {
            return null;
        }
        return "/order/" + bizNo;
    }

    private void attachOwnedOrderActions(Long platformUserId, Long tenantId, List<AssetHoldVO> holds) {
        Set<String> orderNos = holds.stream()
                .filter(hold -> "SALES_ORDER".equals(hold.getBizType()))
                .map(AssetHoldVO::getBizNo)
                .filter(Objects::nonNull)
                .filter(bizNo -> !bizNo.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (orderNos.isEmpty()) {
            return;
        }
        Map<String, SalesOrder> orders = salesOrderMapper.selectList(new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getPlatformUserId, platformUserId)
                        .eq(tenantId != null, SalesOrder::getTenantId, tenantId)
                        .in(SalesOrder::getOrderNo, orderNos))
                .stream()
                .filter(order -> order.getOrderNo() != null)
                .collect(Collectors.toMap(SalesOrder::getOrderNo, Function.identity(), (left, right) -> left));
        holds.forEach(hold -> {
            SalesOrder order = orders.get(hold.getBizNo());
            if (order == null || !Objects.equals(order.getTenantId(), hold.getTenantId())) {
                hold.setActionPath(null);
                hold.setActionLabel(null);
                return;
            }
            hold.setActionPath("/order/" + order.getOrderNo());
            hold.setActionLabel(orderActionLabel(order.getOrderStatus()));
        });
    }

    private String orderActionLabel(String orderStatus) {
        if ("CANCELLED".equals(orderStatus) || "CLOSED".equals(orderStatus)) {
            return "订单已取消";
        }
        if ("PAID".equals(orderStatus)) {
            return "订单已支付";
        }
        return "查看订单";
    }

    private ActivityQuery normalizeQuery(AssetActivityQueryDTO query) {
        AssetActivityQueryDTO source = query == null ? new AssetActivityQueryDTO() : query;
        int size = source.getSize() == null ? 20 : source.getSize();
        if (size < 1 || size > 50) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "每页条数必须在1到50之间");
        }
        if (source.getTenantId() != null && source.getTenantId() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "商户ID必须大于0");
        }
        if (!ActivityCursorUtil.isValidTypes(source.getTypes())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "资产类型参数错误");
        }
        if (source.getFrom() != null && source.getTo() != null) {
            if (source.getFrom().isAfter(source.getTo())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "开始时间不能晚于结束时间");
            }
            if (source.getFrom().plusDays(366).isBefore(source.getTo())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "查询时间范围不能超过366天");
            }
        }
        ActivityCursorUtil.DecodedCursor cursor = ActivityCursorUtil.decode(source.getCursor());
        if (source.getCursor() != null && !source.getCursor().isBlank() && cursor == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "游标参数错误");
        }
        Set<String> types = source.getTypes() == null || source.getTypes().isEmpty()
                ? ASSET_TYPES
                : Set.copyOf(source.getTypes());
        return new ActivityQuery(types, source.getTenantId(), source.getFrom(), source.getTo(), cursor, size);
    }

    private void appendWalletActivities(Long platformUserId,
                                        ActivityQuery query,
                                        int candidateLimit,
                                        List<AppAssetActivityVO> activities) {
        if (query.tenantId() == null) {
            QueryWrapper<UnifiedWalletLog> unifiedWrapper = new QueryWrapper<UnifiedWalletLog>()
                    .eq("platform_user_id", platformUserId);
            applyActivityBounds(unifiedWrapper, "create_time", "UNIFIED_WALLET_LOG", query);
            unifiedWalletLogMapper.selectPage(new Page<>(1, candidateLimit, false),
                            unifiedWrapper.orderByDesc("create_time").orderByDesc("id"))
                    .getRecords()
                    .forEach(log -> activities.add(fromUnifiedWallet(log)));
        }

        QueryWrapper<MerchantWalletLog> merchantWrapper = new QueryWrapper<MerchantWalletLog>()
                .eq("platform_user_id", platformUserId)
                .eq(query.tenantId() != null, "tenant_id", query.tenantId());
        applyActivityBounds(merchantWrapper, "create_time", "MERCHANT_WALLET_LOG", query);
        merchantWalletLogMapper.selectPage(new Page<>(1, candidateLimit, false),
                        merchantWrapper.orderByDesc("create_time").orderByDesc("id"))
                .getRecords()
                .forEach(log -> activities.add(fromMerchantWallet(log)));
    }

    private void appendPointsActivities(Long platformUserId,
                                        ActivityQuery query,
                                        int candidateLimit,
                                        List<AppAssetActivityVO> activities) {
        String occurredAt = "CASE WHEN status = 'RELEASED' AND release_time IS NOT NULL "
                + "THEN release_time ELSE COALESCE(confirm_time, create_time) END";
        QueryWrapper<MemberPointsLog> pointsWrapper = new QueryWrapper<MemberPointsLog>()
                .eq("platform_user_id", platformUserId)
                .eq(query.tenantId() != null, "tenant_id", query.tenantId())
                .apply("(status IS NULL OR status <> {0} OR change_points IS NULL OR change_points <= 0)",
                        PointsDeductStatusEnum.EXPIRED.name());
        applyActivityBounds(pointsWrapper, occurredAt, "MEMBER_POINTS_LOG", query);
        memberPointsLogMapper.selectPage(new Page<>(1, candidateLimit, false),
                        pointsWrapper.orderByDesc(occurredAt).orderByDesc("id"))
                .getRecords()
                .stream()
                .map(this::fromPoints)
                .filter(Objects::nonNull)
                .forEach(activities::add);
    }

    private void appendGrowthActivities(Long platformUserId,
                                        ActivityQuery query,
                                        int candidateLimit,
                                        List<AppAssetActivityVO> activities) {
        QueryWrapper<MemberGrowthLog> growthWrapper = new QueryWrapper<MemberGrowthLog>()
                .eq("platform_user_id", platformUserId)
                .eq(query.tenantId() != null, "tenant_id", query.tenantId());
        applyActivityBounds(growthWrapper, "create_time", "MEMBER_GROWTH_LOG", query);
        memberGrowthLogMapper.selectPage(new Page<>(1, candidateLimit, false),
                        growthWrapper.orderByDesc("create_time").orderByDesc("id"))
                .getRecords()
                .forEach(log -> activities.add(fromGrowth(log)));
    }

    private void appendCouponActivities(Long platformUserId,
                                        ActivityQuery query,
                                        int candidateLimit,
                                        List<AppAssetActivityVO> activities) {
        QueryWrapper<CouponReceiveRecord> receiveWrapper = new QueryWrapper<CouponReceiveRecord>()
                .eq("platform_user_id", platformUserId)
                .eq(query.tenantId() != null, "tenant_id", query.tenantId());
        applyActivityBounds(receiveWrapper, "receive_time", "COUPON_RECEIVE_RECORD", query);
        couponReceiveRecordMapper.selectPage(new Page<>(1, candidateLimit, false),
                        receiveWrapper.orderByDesc("receive_time").orderByDesc("id"))
                .getRecords()
                .forEach(record -> activities.add(activity("COUPON", "优惠券领取", "优惠券已进入账户",
                        record.getReceiveTime(), record.getTenantId(), "COUPON_RECEIVE", record.getBizNo(), null,
                        "positive", couponPath(record.getTenantId(), "my"), "COUPON_RECEIVE_RECORD", record.getId())));

        QueryWrapper<CouponLockRecord> lockWrapper = new QueryWrapper<CouponLockRecord>()
                .eq("platform_user_id", platformUserId)
                .eq(query.tenantId() != null, "tenant_id", query.tenantId());
        applyActivityBounds(lockWrapper, "lock_time", "COUPON_LOCK_RECORD", query);
        couponLockRecordMapper.selectPage(new Page<>(1, candidateLimit, false),
                        lockWrapper.orderByDesc("lock_time").orderByDesc("id"))
                .getRecords()
                .forEach(record -> activities.add(activity("COUPON", "优惠券锁定", "订单 " + record.getOrderNo() + " 锁定优惠券",
                        record.getLockTime(), record.getTenantId(), "SALES_ORDER", record.getOrderNo(), null,
                        "neutral", couponPath(record.getTenantId(), "my"), "COUPON_LOCK_RECORD", record.getId())));

        QueryWrapper<CouponReleaseRecord> releaseWrapper = new QueryWrapper<CouponReleaseRecord>()
                .eq("platform_user_id", platformUserId)
                .eq(query.tenantId() != null, "tenant_id", query.tenantId());
        applyActivityBounds(releaseWrapper, "release_time", "COUPON_RELEASE_RECORD", query);
        couponReleaseRecordMapper.selectPage(new Page<>(1, candidateLimit, false),
                        releaseWrapper.orderByDesc("release_time").orderByDesc("id"))
                .getRecords()
                .forEach(record -> activities.add(activity("COUPON", "优惠券释放", trimToFallback(record.getReleaseReason(), "优惠券已释放"),
                        record.getReleaseTime(), record.getTenantId(), "SALES_ORDER", record.getOrderNo(), null,
                        "neutral", couponPath(record.getTenantId(), "my"), "COUPON_RELEASE_RECORD", record.getId())));

        QueryWrapper<CouponWriteOffRecord> writeOffWrapper = new QueryWrapper<CouponWriteOffRecord>()
                .eq("platform_user_id", platformUserId)
                .eq(query.tenantId() != null, "tenant_id", query.tenantId());
        applyActivityBounds(writeOffWrapper, "write_off_time", "COUPON_WRITE_OFF_RECORD", query);
        couponWriteOffRecordMapper.selectPage(new Page<>(1, candidateLimit, false),
                        writeOffWrapper.orderByDesc("write_off_time").orderByDesc("id"))
                .getRecords()
                .forEach(record -> activities.add(activity("COUPON", "优惠券核销", "订单 " + record.getOrderNo() + " 已使用优惠券",
                        record.getWriteOffTime(), record.getTenantId(), "SALES_ORDER", record.getOrderNo(), moneyText(record.getDiscountAmount()),
                        "positive", couponPath(record.getTenantId(), "expired"), "COUPON_WRITE_OFF_RECORD", record.getId())));
        appendCouponExpireActivities(platformUserId, query, candidateLimit, activities);
    }

    private void appendCouponExpireActivities(Long platformUserId,
                                              ActivityQuery query,
                                              int candidateLimit,
                                              List<AppAssetActivityVO> activities) {
        QueryWrapper<CouponExpireRecord> expireWrapper = new QueryWrapper<CouponExpireRecord>()
                .eq("platform_user_id", platformUserId)
                .eq(query.tenantId() != null, "tenant_id", query.tenantId());
        applyActivityBounds(expireWrapper, "expire_time", "COUPON_EXPIRE_RECORD", query);
        couponExpireRecordMapper.selectPage(new Page<>(1, candidateLimit, false),
                        expireWrapper.orderByDesc("expire_time").orderByDesc("id"))
                .getRecords()
                .forEach(record -> activities.add(activity("COUPON", "优惠券过期", trimToFallback(record.getExpireReason(), "优惠券已失效"),
                        record.getExpireTime(), record.getTenantId(), "COUPON_EXPIRE", record.getBizNo(), null,
                        "negative", couponPath(record.getTenantId(), "expired"), "COUPON_EXPIRE_RECORD", record.getId())));
    }

    private void applyActivityBounds(QueryWrapper<?> wrapper,
                                     String occurredAtColumn,
                                     String sourceType,
                                     ActivityQuery query) {
        wrapper.ge(query.from() != null, occurredAtColumn, query.from());
        wrapper.le(query.to() != null, occurredAtColumn, query.to());
        if (query.cursor() == null) {
            return;
        }
        int sourceComparison = sourceType.compareTo(query.cursor().sourceType());
        if (sourceComparison > 0) {
            wrapper.le(occurredAtColumn, query.cursor().occurredAt());
        } else if (sourceComparison == 0) {
            wrapper.apply("(" + occurredAtColumn + " < {0} OR (" + occurredAtColumn + " = {1} AND id < {2}))",
                    query.cursor().occurredAt(), query.cursor().occurredAt(), query.cursor().sourceId());
        } else {
            wrapper.lt(occurredAtColumn, query.cursor().occurredAt());
        }
    }

    private boolean matchesQuery(AppAssetActivityVO activity, ActivityQuery query) {
        if (!query.includes(activity.getAssetType())) {
            return false;
        }
        if (query.tenantId() != null && !Objects.equals(query.tenantId(), activity.getTenantId())) {
            return false;
        }
        if (query.from() != null && activity.getOccurredAt().isBefore(query.from())) {
            return false;
        }
        if (query.to() != null && activity.getOccurredAt().isAfter(query.to())) {
            return false;
        }
        return isAfterCursor(activity, query.cursor());
    }

    private boolean isAfterCursor(AppAssetActivityVO activity, ActivityCursorUtil.DecodedCursor cursor) {
        if (cursor == null) {
            return true;
        }
        int occurredAtComparison = activity.getOccurredAt().compareTo(cursor.occurredAt());
        if (occurredAtComparison != 0) {
            return occurredAtComparison < 0;
        }
        int sourceTypeComparison = activity.getSourceType().compareTo(cursor.sourceType());
        if (sourceTypeComparison != 0) {
            return sourceTypeComparison > 0;
        }
        return activity.getSourceId() != null && activity.getSourceId() < cursor.sourceId();
    }

    private AppAssetActivityVO last(List<AppAssetActivityVO> records) {
        return records.get(records.size() - 1);
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
                log.getCreateTime(), null, log.getBizType(), log.getBizNo(), moneyText(log.getChangeAmount()),
                walletLog.getTrace().getTone(), "/history", "UNIFIED_WALLET_LOG", log.getId());
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
                log.getCreateTime(), log.getTenantId(), log.getBizType(), log.getBizNo(), moneyText(log.getChangeAmount()),
                walletLog.getTrace().getTone(), log.getTenantId() == null ? "/history" : "/wallet/tenants/" + log.getTenantId(),
                "MERCHANT_WALLET_LOG", log.getId());
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
                    log.getTenantId(), log.getBizType(), log.getBizNo(), "+" + points + " 分", "positive",
                    log.getTenantId() == null ? "/wallet" : "/points/" + log.getTenantId(), "MEMBER_POINTS_LOG", log.getId());
        }
        boolean preHold = PointsDeductStatusEnum.PRE_HOLD.name().equals(status);
        String title = log.getChangePoints() == null || log.getChangePoints() >= 0 ? "积分入账" : "积分扣减";
        if (preHold) {
            title = "积分已预扣";
        }
        String amount = log.getChangePoints() == null ? null : (log.getChangePoints() > 0 ? "+" : "") + log.getChangePoints() + " 分";
        return activity("POINTS", title, trimToFallback(log.getRemark(), trimToFallback(log.getBizType(), "积分变动")),
                log.getConfirmTime() == null ? log.getCreateTime() : log.getConfirmTime(),
                log.getTenantId(), log.getBizType(), log.getBizNo(), amount,
                preHold ? "neutral" : log.getChangePoints() != null && log.getChangePoints() < 0 ? "negative" : "positive",
                log.getTenantId() == null ? "/wallet" : "/points/" + log.getTenantId(), "MEMBER_POINTS_LOG", log.getId());
    }

    private AppAssetActivityVO fromGrowth(MemberGrowthLog log) {
        String title = log.getChangeGrowth() == null || log.getChangeGrowth() >= 0 ? "成长值增加" : "成长值扣减";
        String amount = log.getChangeGrowth() == null ? null : (log.getChangeGrowth() > 0 ? "+" : "") + log.getChangeGrowth();
        return activity("GROWTH", title, trimToFallback(log.getRemark(), trimToFallback(log.getBizType(), "成长值变动")),
                log.getCreateTime(), log.getTenantId(), log.getBizType(), log.getBizNo(), amount,
                log.getChangeGrowth() != null && log.getChangeGrowth() < 0 ? "negative" : "positive",
                log.getTenantId() == null ? "/wallet" : "/growth/" + log.getTenantId(), "MEMBER_GROWTH_LOG", log.getId());
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
                                        String bizType,
                                        String bizNo,
                                        String amountText,
                                        String tone,
                                        String actionPath,
                                        String sourceType,
                                        Long sourceId) {
        AppAssetActivityVO vo = new AppAssetActivityVO();
        vo.setAssetType(assetType);
        vo.setTitle(title);
        vo.setDescription(description);
        vo.setOccurredAt(occurredAt);
        vo.setTenantId(tenantId);
        vo.setBizType(bizType);
        vo.setBizNo(bizNo);
        vo.setAmountText(amountText);
        vo.setTone(tone);
        vo.setActionPath(actionPath);
        vo.setSourceType(sourceType);
        vo.setSourceId(sourceId);
        return vo;
    }

    private record ActivityQuery(Set<String> types,
                                 Long tenantId,
                                 LocalDateTime from,
                                 LocalDateTime to,
                                 ActivityCursorUtil.DecodedCursor cursor,
                                 int size) {
        private boolean includes(String assetType) {
            return types.contains(assetType);
        }
    }

    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return (amount.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + "¥" + amount.stripTrailingZeros().toPlainString();
    }

    private String absoluteMoneyText(BigDecimal amount) {
        return "¥" + amount.abs().stripTrailingZeros().toPlainString();
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
