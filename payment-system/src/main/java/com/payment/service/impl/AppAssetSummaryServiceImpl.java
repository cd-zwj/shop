package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.payment.dto.AppTenantAssetSummaryVO;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.MemberPointsLog;
import com.payment.entity.MemberGrowthLog;
import com.payment.entity.MerchantWalletAccount;
import com.payment.entity.Tenant;
import com.payment.entity.TenantMember;
import com.payment.enums.PointsDeductStatusEnum;
import com.payment.enums.UserCouponStatusEnum;
import com.payment.mapper.MemberGrowthLogMapper;
import com.payment.mapper.MemberPointsAccountMapper;
import com.payment.mapper.MemberPointsLogMapper;
import com.payment.mapper.MerchantWalletAccountMapper;
import com.payment.mapper.TenantMapper;
import com.payment.mapper.TenantMemberMapper;
import com.payment.mapper.UserCouponMapper;
import com.payment.service.AppAssetSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
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
                .map(tenantId -> toSummary(tenantId, tenantMap, memberMap, walletMap, pointsMap, platformUserId))
                .toList();
    }

    private AppTenantAssetSummaryVO toSummary(Long tenantId,
                                              Map<Long, Tenant> tenantMap,
                                              Map<Long, TenantMember> memberMap,
                                              Map<Long, MerchantWalletAccount> walletMap,
                                              Map<Long, MemberPointsAccount> pointsMap,
                                              Long platformUserId) {
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
        vo.setExpiringSoonPoints(Math.min(availablePoints, sumExpiringPoints(tenantId, platformUserId)));
        vo.setUsableCouponCount(countCoupons(tenantId, platformUserId,
                UserCouponStatusEnum.RECEIVED.name(), UserCouponStatusEnum.RELEASED.name()));
        vo.setLockedCouponCount(countCoupons(tenantId, platformUserId, UserCouponStatusEnum.LOCKED.name()));
        vo.setUsedCouponCount(countCoupons(tenantId, platformUserId, UserCouponStatusEnum.USED.name()));
        vo.setExpiredCouponCount(countCoupons(tenantId, platformUserId, UserCouponStatusEnum.EXPIRED.name()));
        vo.setTotalGrowth(sumGrowth(tenantId, platformUserId));
        return vo;
    }

    private int countCoupons(Long tenantId, Long platformUserId, String... statuses) {
        Long count = userCouponMapper.selectCount(new LambdaQueryWrapper<com.payment.entity.UserCoupon>()
                .eq(com.payment.entity.UserCoupon::getTenantId, tenantId)
                .eq(com.payment.entity.UserCoupon::getPlatformUserId, platformUserId)
                .in(com.payment.entity.UserCoupon::getCouponStatus, List.of(statuses)));
        return count == null ? 0 : count.intValue();
    }

    private int sumGrowth(Long tenantId, Long platformUserId) {
        QueryWrapper<MemberGrowthLog> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId)
                .eq("platform_user_id", platformUserId)
                .select("IFNULL(SUM(change_growth), 0) AS change_growth");
        MemberGrowthLog result = memberGrowthLogMapper.selectOne(wrapper);
        return result == null || result.getChangeGrowth() == null ? 0 : Math.max(0, result.getChangeGrowth());
    }

    private int sumExpiringPoints(Long tenantId, Long platformUserId) {
        LocalDateTime now = LocalDateTime.now();
        return memberPointsLogMapper.selectList(new LambdaQueryWrapper<MemberPointsLog>()
                        .eq(MemberPointsLog::getTenantId, tenantId)
                        .eq(MemberPointsLog::getPlatformUserId, platformUserId)
                        .gt(MemberPointsLog::getChangePoints, 0)
                        .eq(MemberPointsLog::getStatus, PointsDeductStatusEnum.CONFIRMED.name())
                        .isNotNull(MemberPointsLog::getExpireTime)
                        .ge(MemberPointsLog::getExpireTime, now)
                        .le(MemberPointsLog::getExpireTime, now.plusDays(30)))
                .stream()
                .map(MemberPointsLog::getChangePoints)
                .filter(points -> points != null && points > 0)
                .mapToInt(Integer::intValue)
                .sum();
    }
}
