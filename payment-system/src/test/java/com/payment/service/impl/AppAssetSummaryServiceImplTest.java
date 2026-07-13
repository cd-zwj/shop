package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.AppAssetActivityVO;
import com.payment.dto.AppTenantAssetSummaryVO;
import com.payment.dto.AssetActivityPageVO;
import com.payment.dto.AssetActivityQueryDTO;
import com.payment.dto.AssetHoldVO;
import com.payment.entity.CouponLockRecord;
import com.payment.entity.CouponReceiveRecord;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.MemberPointsLog;
import com.payment.entity.MemberGrowthLog;
import com.payment.entity.MerchantWalletAccount;
import com.payment.entity.MerchantWalletLog;
import com.payment.entity.SalesOrder;
import com.payment.entity.Tenant;
import com.payment.entity.TenantMember;
import com.payment.entity.UnifiedWalletLog;
import com.payment.entity.UserCoupon;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppAssetSummaryServiceImplTest {

    @Test
    void listTenantAssetSummariesShouldMergeMembershipWalletAndPointsWithoutCreatingAccounts() {
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        TenantMapper tenantMapper = mock(TenantMapper.class);
        MerchantWalletAccountMapper walletAccountMapper = mock(MerchantWalletAccountMapper.class);
        MemberPointsAccountMapper pointsAccountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper pointsLogMapper = mock(MemberPointsLogMapper.class);
        MemberGrowthLogMapper growthLogMapper = mock(MemberGrowthLogMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);

        when(tenantMemberMapper.selectList(any())).thenReturn(List.of(member(1L, 1), member(3L, 1)));
        when(walletAccountMapper.selectList(any())).thenReturn(List.of(wallet(2L, "12.50", "1.00")));
        when(pointsAccountMapper.selectList(any())).thenReturn(List.of(points(1L, 120), points(2L, 80)));
        when(tenantMapper.selectBatchIds(any())).thenReturn(List.of(
                tenant(1L, "咖啡店"),
                tenant(2L, "书店"),
                tenant(3L, "花店"),
                tenant(4L, "甜品店")
        ));
        when(userCouponMapper.selectMaps(any()))
                .thenReturn(List.of(
                        row("tenantId", 1L, "couponStatus", "RECEIVED", "count", 2),
                        row("tenantId", 1L, "couponStatus", "USED", "count", 1),
                        row("tenantId", 2L, "couponStatus", "RECEIVED", "count", 3),
                        row("tenantId", 2L, "couponStatus", "LOCKED", "count", 1),
                        row("tenantId", 2L, "couponStatus", "USED", "count", 2),
                        row("tenantId", 2L, "couponStatus", "EXPIRED", "count", 1),
                        row("tenantId", 4L, "couponStatus", "RECEIVED", "count", 1)))
                .thenReturn(List.of(
                        row("tenantId", 1L, "count", 1),
                        row("tenantId", 2L, "count", 2)));
        when(growthLogMapper.selectMaps(any())).thenReturn(List.of(
                row("tenantId", 1L, "totalGrowth", 260),
                row("tenantId", 2L, "totalGrowth", 80),
                row("tenantId", 4L, "totalGrowth", 40)));
        when(pointsLogMapper.selectMaps(any())).thenReturn(List.of(
                row("tenantId", 1L, "points", 50),
                row("tenantId", 2L, "points", 200)));

        AppAssetSummaryServiceImpl service = service(
                tenantMemberMapper,
                tenantMapper,
                walletAccountMapper,
                pointsAccountMapper,
                pointsLogMapper,
                growthLogMapper,
                userCouponMapper);

        List<AppTenantAssetSummaryVO> result = service.listTenantAssetSummaries(99L);

        assertThat(result).extracting(AppTenantAssetSummaryVO::getTenantId)
                .containsExactly(1L, 2L, 3L, 4L);
        assertThat(result.get(0).getTenantName()).isEqualTo("咖啡店");
        assertThat(result.get(0).getPoints()).isEqualTo(120);
        assertThat(result.get(0).getExpiringSoonPoints()).isEqualTo(50);
        assertThat(result.get(0).getMemberStatus()).isEqualTo(1);
        assertThat(result.get(0).getUsableCouponCount()).isEqualTo(2);
        assertThat(result.get(0).getUsedCouponCount()).isEqualTo(1);
        assertThat(result.get(0).getExpiringSoonCouponCount()).isEqualTo(1);
        assertThat(result.get(0).getTotalGrowth()).isEqualTo(260);
        assertThat(result.get(1).getWalletAvailableAmount()).isEqualByComparingTo("12.50");
        assertThat(result.get(1).getWalletFrozenAmount()).isEqualByComparingTo("1.00");
        assertThat(result.get(1).getPoints()).isEqualTo(80);
        assertThat(result.get(1).getExpiringSoonPoints()).isEqualTo(80);
        assertThat(result.get(1).getUsableCouponCount()).isEqualTo(3);
        assertThat(result.get(1).getLockedCouponCount()).isEqualTo(1);
        assertThat(result.get(1).getExpiringSoonCouponCount()).isEqualTo(2);
        assertThat(result.get(1).getTotalGrowth()).isEqualTo(80);
        assertThat(result.get(2).getPoints()).isZero();
        assertThat(result.get(3).getTenantName()).isEqualTo("甜品店");
        assertThat(result.get(3).getUsableCouponCount()).isEqualTo(1);
        assertThat(result.get(3).getTotalGrowth()).isEqualTo(40);
        verify(userCouponMapper, never()).selectCount(any());
        verify(growthLogMapper, never()).selectOne(any());
    }

    @Test
    void listTenantAssetSummariesShouldReturnEmptyWhenUserHasNoTenantAssets() {
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        TenantMapper tenantMapper = mock(TenantMapper.class);
        MerchantWalletAccountMapper walletAccountMapper = mock(MerchantWalletAccountMapper.class);
        MemberPointsAccountMapper pointsAccountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper pointsLogMapper = mock(MemberPointsLogMapper.class);
        MemberGrowthLogMapper growthLogMapper = mock(MemberGrowthLogMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);

        when(tenantMemberMapper.selectList(any())).thenReturn(List.of());
        when(walletAccountMapper.selectList(any())).thenReturn(List.of());
        when(pointsAccountMapper.selectList(any())).thenReturn(List.of());
        when(userCouponMapper.selectMaps(any())).thenReturn(List.of());
        when(growthLogMapper.selectMaps(any())).thenReturn(List.of());
        when(pointsLogMapper.selectMaps(any())).thenReturn(List.of());

        AppAssetSummaryServiceImpl service = service(
                tenantMemberMapper,
                tenantMapper,
                walletAccountMapper,
                pointsAccountMapper,
                pointsLogMapper,
                growthLogMapper,
                userCouponMapper);

        assertThat(service.listTenantAssetSummaries(99L)).isEmpty();
    }

    @Test
    void listAssetActivitiesShouldMergeAssetEventsByTime() {
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        TenantMapper tenantMapper = mock(TenantMapper.class);
        MerchantWalletAccountMapper walletAccountMapper = mock(MerchantWalletAccountMapper.class);
        MemberPointsAccountMapper pointsAccountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper pointsLogMapper = mock(MemberPointsLogMapper.class);
        MemberGrowthLogMapper growthLogMapper = mock(MemberGrowthLogMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        UnifiedWalletLogMapper unifiedWalletLogMapper = mock(UnifiedWalletLogMapper.class);
        MerchantWalletLogMapper merchantWalletLogMapper = mock(MerchantWalletLogMapper.class);
        CouponReceiveRecordMapper receiveRecordMapper = mock(CouponReceiveRecordMapper.class);
        CouponLockRecordMapper lockRecordMapper = mock(CouponLockRecordMapper.class);
        CouponReleaseRecordMapper releaseRecordMapper = mock(CouponReleaseRecordMapper.class);
        CouponWriteOffRecordMapper writeOffRecordMapper = mock(CouponWriteOffRecordMapper.class);
        CouponExpireRecordMapper expireRecordMapper = mock(CouponExpireRecordMapper.class);

        when(unifiedWalletLogMapper.selectPage(any(), any())).thenReturn(page(unifiedWallet(
                LocalDateTime.of(2026, 7, 10, 10, 0), "ORDER_PAY", "SO1001", "-10.00")));
        when(merchantWalletLogMapper.selectPage(any(), any())).thenReturn(page(merchantWallet(
                LocalDateTime.of(2026, 7, 10, 11, 0), 9L, "REFUND", "SO1002", "3.00")));
        when(pointsLogMapper.selectPage(any(), any())).thenReturn(page(List.of(
                releasedPointsLog(LocalDateTime.of(2026, 7, 10, 13, 0), 9L, 20),
                pointsLog(LocalDateTime.of(2026, 7, 10, 9, 0), 9L, 20))));
        when(growthLogMapper.selectPage(any(), any())).thenReturn(page(growthLog(
                LocalDateTime.of(2026, 7, 10, 8, 0), 9L, 5)));
        when(receiveRecordMapper.selectPage(any(), any())).thenReturn(page(receiveRecord(
                LocalDateTime.of(2026, 7, 10, 12, 0), 9L, "CR1001")));
        when(lockRecordMapper.selectPage(any(), any())).thenReturn(page(lockRecord(
                LocalDateTime.of(2026, 7, 10, 7, 0), 9L, "SO1001")));
        when(releaseRecordMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 2));
        when(writeOffRecordMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 2));
        when(expireRecordMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 2));
        when(tenantMapper.selectBatchIds(any())).thenReturn(List.of(tenant(9L, "本地测试店")));

        AppAssetSummaryServiceImpl service = new AppAssetSummaryServiceImpl(
                tenantMemberMapper,
                tenantMapper,
                walletAccountMapper,
                pointsAccountMapper,
                pointsLogMapper,
                growthLogMapper,
                userCouponMapper,
                unifiedWalletLogMapper,
                merchantWalletLogMapper,
                receiveRecordMapper,
                lockRecordMapper,
                releaseRecordMapper,
                writeOffRecordMapper,
                expireRecordMapper,
                mock(SalesOrderMapper.class));

        List<AppAssetActivityVO> result = service.listAssetActivities(99L, 4);

        assertThat(result).extracting(AppAssetActivityVO::getTitle)
                .containsExactly("积分已释放", "优惠券领取", "退款", "ORDER PAY");
        assertThat(result).extracting(AppAssetActivityVO::getTenantName)
                .containsExactly("本地测试店", "本地测试店", "本地测试店", null);
        assertThat(result).extracting(AppAssetActivityVO::getAmountText)
                .containsExactly("+20 分", null, "+¥3", "¥-10");
        assertThat(result).extracting(AppAssetActivityVO::getActionPath)
                .containsExactly("/points/9", "/coupons?tab=my&tenantId=9", "/wallet/tenants/9", "/history");
        verify(userCouponMapper, never()).selectList(any());
        verify(userCouponMapper, never()).selectCount(any());
        ArgumentCaptor<QueryWrapper> lockQueryCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(lockRecordMapper).selectPage(any(), lockQueryCaptor.capture());
        assertThat(lockQueryCaptor.getValue().getCustomSqlSegment())
                .contains("platform_user_id")
                .doesNotContain("user_coupon_id IN");
    }

    @Test
    void listAssetActivitiesLegacyOverloadShouldClampSize() {
        MemberPointsLogMapper pointsLogMapper = mock(MemberPointsLogMapper.class);
        MemberGrowthLogMapper growthLogMapper = mock(MemberGrowthLogMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        UnifiedWalletLogMapper unifiedWalletLogMapper = mock(UnifiedWalletLogMapper.class);
        MerchantWalletLogMapper merchantWalletLogMapper = mock(MerchantWalletLogMapper.class);
        CouponReceiveRecordMapper receiveRecordMapper = mock(CouponReceiveRecordMapper.class);
        CouponLockRecordMapper lockRecordMapper = mock(CouponLockRecordMapper.class);
        CouponReleaseRecordMapper releaseRecordMapper = mock(CouponReleaseRecordMapper.class);
        CouponWriteOffRecordMapper writeOffRecordMapper = mock(CouponWriteOffRecordMapper.class);
        CouponExpireRecordMapper expireRecordMapper = mock(CouponExpireRecordMapper.class);
        when(unifiedWalletLogMapper.selectPage(any(), any())).thenReturn(new Page<>());
        when(merchantWalletLogMapper.selectPage(any(), any())).thenReturn(new Page<>());
        when(pointsLogMapper.selectPage(any(), any())).thenReturn(new Page<>());
        when(growthLogMapper.selectPage(any(), any())).thenReturn(new Page<>());
        when(receiveRecordMapper.selectPage(any(), any())).thenReturn(new Page<>());
        when(lockRecordMapper.selectPage(any(), any())).thenReturn(new Page<>());
        when(releaseRecordMapper.selectPage(any(), any())).thenReturn(new Page<>());
        when(writeOffRecordMapper.selectPage(any(), any())).thenReturn(new Page<>());
        when(expireRecordMapper.selectPage(any(), any())).thenReturn(new Page<>());

        AppAssetSummaryServiceImpl service = new AppAssetSummaryServiceImpl(
                mock(TenantMemberMapper.class), mock(TenantMapper.class), mock(MerchantWalletAccountMapper.class),
                mock(MemberPointsAccountMapper.class), pointsLogMapper, growthLogMapper, userCouponMapper,
                unifiedWalletLogMapper, merchantWalletLogMapper, receiveRecordMapper, lockRecordMapper,
                releaseRecordMapper, writeOffRecordMapper, expireRecordMapper,
                mock(SalesOrderMapper.class));

        assertThat(service.listAssetActivities(99L, 0)).isEmpty();
        assertThat(service.listAssetActivities(99L, 51)).isEmpty();
    }

    @Test
    void listAssetActivitiesShouldPageStablyForEventsWithTheSameTimestamp() {
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        TenantMapper tenantMapper = mock(TenantMapper.class);
        MerchantWalletAccountMapper walletAccountMapper = mock(MerchantWalletAccountMapper.class);
        MemberPointsAccountMapper pointsAccountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper pointsLogMapper = mock(MemberPointsLogMapper.class);
        MemberGrowthLogMapper growthLogMapper = mock(MemberGrowthLogMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        UnifiedWalletLogMapper unifiedWalletLogMapper = mock(UnifiedWalletLogMapper.class);
        MerchantWalletLogMapper merchantWalletLogMapper = mock(MerchantWalletLogMapper.class);
        CouponReceiveRecordMapper receiveRecordMapper = mock(CouponReceiveRecordMapper.class);
        CouponLockRecordMapper lockRecordMapper = mock(CouponLockRecordMapper.class);
        CouponReleaseRecordMapper releaseRecordMapper = mock(CouponReleaseRecordMapper.class);
        CouponWriteOffRecordMapper writeOffRecordMapper = mock(CouponWriteOffRecordMapper.class);
        CouponExpireRecordMapper expireRecordMapper = mock(CouponExpireRecordMapper.class);
        LocalDateTime occurredAt = LocalDateTime.of(2026, 7, 11, 10, 0);

        when(unifiedWalletLogMapper.selectPage(any(), any())).thenReturn(page(List.of(
                unifiedWallet(9L, occurredAt, "ORDER_PAY", "SO1001", "-10.00"),
                unifiedWallet(1L, occurredAt, "ORDER_PAY", "SO1002", "-8.00"))));
        when(merchantWalletLogMapper.selectPage(any(), any())).thenReturn(page(merchantWallet(2L, occurredAt, 9L, "REFUND", "SO1002", "3.00")));
        when(pointsLogMapper.selectPage(any(), any())).thenReturn(page(pointsLog(3L, occurredAt, 9L, 20)));
        when(growthLogMapper.selectPage(any(), any())).thenReturn(page(growthLog(4L, occurredAt, 9L, 5)));
        when(receiveRecordMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 3));
        when(lockRecordMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 3));
        when(releaseRecordMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 3));
        when(writeOffRecordMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 3));
        when(expireRecordMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 3));
        when(tenantMapper.selectBatchIds(any())).thenReturn(List.of(tenant(9L, "本地测试店")));

        AppAssetSummaryServiceImpl service = new AppAssetSummaryServiceImpl(
                tenantMemberMapper, tenantMapper, walletAccountMapper, pointsAccountMapper, pointsLogMapper,
                growthLogMapper, userCouponMapper, unifiedWalletLogMapper, merchantWalletLogMapper,
                receiveRecordMapper, lockRecordMapper, releaseRecordMapper, writeOffRecordMapper, expireRecordMapper,
                mock(SalesOrderMapper.class));

        AssetActivityQueryDTO firstQuery = new AssetActivityQueryDTO();
        firstQuery.setSize(2);
        AssetActivityPageVO firstPage = service.listAssetActivities(99L, firstQuery);

        assertThat(firstPage.getRecords()).extracting(AppAssetActivityVO::getSourceType)
                .containsExactly("MEMBER_GROWTH_LOG", "MEMBER_POINTS_LOG");
        assertThat(firstPage.isHasMore()).isTrue();
        assertThat(firstPage.getNextCursor()).isNotBlank();

        AssetActivityQueryDTO nextQuery = new AssetActivityQueryDTO();
        nextQuery.setSize(2);
        nextQuery.setCursor(firstPage.getNextCursor());
        AssetActivityPageVO secondPage = service.listAssetActivities(99L, nextQuery);

        assertThat(secondPage.getRecords()).extracting(AppAssetActivityVO::getSourceType)
                .containsExactly("MERCHANT_WALLET_LOG", "UNIFIED_WALLET_LOG");
        assertThat(secondPage.getRecords().get(1).getSourceId()).isEqualTo(9L);
        assertThat(secondPage.isHasMore()).isTrue();

        AssetActivityQueryDTO finalQuery = new AssetActivityQueryDTO();
        finalQuery.setSize(2);
        finalQuery.setCursor(secondPage.getNextCursor());
        AssetActivityPageVO finalPage = service.listAssetActivities(99L, finalQuery);

        assertThat(finalPage.getRecords()).singleElement()
                .extracting(AppAssetActivityVO::getSourceType, AppAssetActivityVO::getSourceId)
                .containsExactly("UNIFIED_WALLET_LOG", 1L);
        assertThat(finalPage.isHasMore()).isFalse();
    }

    @Test
    void listAssetActivitiesShouldApplyTypeAndTenantFiltersAndRejectInvalidQueries() {
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        TenantMapper tenantMapper = mock(TenantMapper.class);
        MerchantWalletAccountMapper walletAccountMapper = mock(MerchantWalletAccountMapper.class);
        MemberPointsAccountMapper pointsAccountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper pointsLogMapper = mock(MemberPointsLogMapper.class);
        MemberGrowthLogMapper growthLogMapper = mock(MemberGrowthLogMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        UnifiedWalletLogMapper unifiedWalletLogMapper = mock(UnifiedWalletLogMapper.class);
        MerchantWalletLogMapper merchantWalletLogMapper = mock(MerchantWalletLogMapper.class);
        CouponReceiveRecordMapper receiveRecordMapper = mock(CouponReceiveRecordMapper.class);
        CouponLockRecordMapper lockRecordMapper = mock(CouponLockRecordMapper.class);
        CouponReleaseRecordMapper releaseRecordMapper = mock(CouponReleaseRecordMapper.class);
        CouponWriteOffRecordMapper writeOffRecordMapper = mock(CouponWriteOffRecordMapper.class);
        CouponExpireRecordMapper expireRecordMapper = mock(CouponExpireRecordMapper.class);
        LocalDateTime occurredAt = LocalDateTime.of(2026, 7, 11, 10, 0);

        when(pointsLogMapper.selectPage(any(), any())).thenReturn(page(pointsLog(3L, occurredAt, 9L, 20)));
        when(tenantMapper.selectBatchIds(any())).thenReturn(List.of(tenant(9L, "本地测试店")));

        AppAssetSummaryServiceImpl service = new AppAssetSummaryServiceImpl(
                tenantMemberMapper, tenantMapper, walletAccountMapper, pointsAccountMapper, pointsLogMapper,
                growthLogMapper, userCouponMapper, unifiedWalletLogMapper, merchantWalletLogMapper,
                receiveRecordMapper, lockRecordMapper, releaseRecordMapper, writeOffRecordMapper, expireRecordMapper,
                mock(SalesOrderMapper.class));

        AssetActivityQueryDTO query = new AssetActivityQueryDTO();
        query.setTypes(List.of("POINTS"));
        query.setTenantId(9L);
        query.setFrom(occurredAt.minusMinutes(1));
        query.setTo(occurredAt.plusMinutes(1));
        query.setSize(20);

        AssetActivityPageVO result = service.listAssetActivities(99L, query);

        assertThat(result.getRecords()).singleElement()
                .extracting(AppAssetActivityVO::getAssetType, AppAssetActivityVO::getTenantId)
                .containsExactly("POINTS", 9L);

        AssetActivityQueryDTO invalidType = new AssetActivityQueryDTO();
        invalidType.setTypes(List.of("CASH"));
        assertThatThrownBy(() -> service.listAssetActivities(99L, invalidType))
                .hasMessage("资产类型参数错误");

        AssetActivityQueryDTO invalidCursor = new AssetActivityQueryDTO();
        invalidCursor.setCursor("not-a-valid-cursor");
        assertThatThrownBy(() -> service.listAssetActivities(99L, invalidCursor))
                .hasMessage("游标参数错误");

        AssetActivityQueryDTO oversized = new AssetActivityQueryDTO();
        oversized.setSize(51);
        assertThatThrownBy(() -> service.listAssetActivities(99L, oversized))
                .hasMessage("每页条数必须在1到50之间");

        AssetActivityQueryDTO foreignTenant = new AssetActivityQueryDTO();
        foreignTenant.setTypes(List.of("POINTS"));
        foreignTenant.setTenantId(10L);
        assertThat(service.listAssetActivities(99L, foreignTenant).getRecords()).isEmpty();
    }

    @Test
    void listAssetHoldsShouldExplainLockedCouponPreHeldPointsAndWalletSummary() {
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        TenantMapper tenantMapper = mock(TenantMapper.class);
        MerchantWalletAccountMapper walletAccountMapper = mock(MerchantWalletAccountMapper.class);
        MemberPointsAccountMapper pointsAccountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper pointsLogMapper = mock(MemberPointsLogMapper.class);
        MemberGrowthLogMapper growthLogMapper = mock(MemberGrowthLogMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponLockRecordMapper lockRecordMapper = mock(CouponLockRecordMapper.class);
        LocalDateTime now = LocalDateTime.of(2026, 7, 11, 10, 30);

        UserCoupon coupon = userCoupon(501L);
        coupon.setTenantId(9L);
        coupon.setPlatformUserId(99L);
        coupon.setCouponStatus("LOCKED");
        coupon.setOrderNo("SO1001");
        coupon.setLockTime(now);
        CouponLockRecord lockRecord = lockRecord(now, 9L, "SO1001");
        lockRecord.setUserCouponId(501L);
        lockRecord.setLockStatus("LOCKED");

        MemberPointsLog preHold = pointsLog(701L, now.minusMinutes(5), 9L, -20);
        preHold.setStatus("PRE_HOLD");
        preHold.setBizType("SALES_ORDER");
        preHold.setBizNo("SO1002");

        MerchantWalletAccount frozenWallet = wallet(9L, "100.00", "30.00");
        frozenWallet.setPlatformUserId(99L);
        frozenWallet.setUpdateTime(now.minusMinutes(10));

        when(userCouponMapper.selectList(any())).thenReturn(List.of(coupon));
        when(lockRecordMapper.selectList(any())).thenReturn(List.of(lockRecord));
        when(pointsLogMapper.selectList(any())).thenReturn(List.of(preHold));
        when(walletAccountMapper.selectList(any())).thenReturn(List.of(frozenWallet));
        when(tenantMapper.selectBatchIds(any())).thenReturn(List.of(tenant(9L, "本地测试店")));
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        when(salesOrderMapper.selectList(any())).thenReturn(List.of(
                salesOrder("SO1001", 9L, 99L, "CREATED"),
                salesOrder("SO1002", 9L, 99L, "CANCELLED")));

        AppAssetSummaryServiceImpl service = new AppAssetSummaryServiceImpl(
                tenantMemberMapper, tenantMapper, walletAccountMapper, pointsAccountMapper, pointsLogMapper,
                growthLogMapper, userCouponMapper, mock(UnifiedWalletLogMapper.class), mock(MerchantWalletLogMapper.class),
                mock(CouponReceiveRecordMapper.class), lockRecordMapper, mock(CouponReleaseRecordMapper.class),
                mock(CouponWriteOffRecordMapper.class), mock(CouponExpireRecordMapper.class), salesOrderMapper);

        List<AssetHoldVO> result = service.listAssetHolds(99L, 9L);

        assertThat(result).extracting(AssetHoldVO::getAssetType, AssetHoldVO::getHoldStatus, AssetHoldVO::getAmountText)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("COUPON", "LOCKED", "1 张优惠券"),
                        org.assertj.core.groups.Tuple.tuple("POINTS", "PRE_HOLD", "-20 积分"),
                        org.assertj.core.groups.Tuple.tuple("WALLET", "FROZEN", "¥30"));
        assertThat(result.get(0).getBizNo()).isEqualTo("SO1001");
        assertThat(result).extracting(AssetHoldVO::getTenantName)
                .containsOnly("本地测试店");
        assertThat(result.get(0).getActionPath()).isEqualTo("/order/SO1001");
        assertThat(result.get(0).getActionLabel()).isEqualTo("查看订单");
        assertThat(result.get(1).getActionPath()).isEqualTo("/order/SO1002");
        assertThat(result.get(1).getActionLabel()).isEqualTo("订单已取消");
        assertThat(result.get(2).getActionPath()).isNull();
        assertThat(result.get(2).getReason()).isEqualTo("冻结金额");
    }

    @Test
    void listAssetHoldsShouldAggregateAllTenantsAndCapResponseAtOneHundred() {
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        TenantMapper tenantMapper = mock(TenantMapper.class);
        MerchantWalletAccountMapper walletAccountMapper = mock(MerchantWalletAccountMapper.class);
        MemberPointsAccountMapper pointsAccountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper pointsLogMapper = mock(MemberPointsLogMapper.class);
        MemberGrowthLogMapper growthLogMapper = mock(MemberGrowthLogMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponLockRecordMapper lockRecordMapper = mock(CouponLockRecordMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        LocalDateTime now = LocalDateTime.of(2026, 7, 11, 12, 0);

        UserCoupon coupon = userCoupon(501L);
        coupon.setTenantId(9L);
        coupon.setPlatformUserId(99L);
        coupon.setCouponStatus("LOCKED");
        coupon.setOrderNo("SO1001");
        coupon.setLockTime(now);

        List<MemberPointsLog> pointHolds = new ArrayList<>();
        for (int index = 0; index < 105; index++) {
            MemberPointsLog hold = pointsLog((long) index + 1, now.minusMinutes(index + 1L),
                    index % 2 == 0 ? 9L : 10L, -10);
            hold.setPlatformUserId(99L);
            hold.setStatus("PRE_HOLD");
            hold.setBizType("SALES_ORDER");
            hold.setBizNo("SO" + (1002 + index));
            pointHolds.add(hold);
        }

        when(userCouponMapper.selectList(any())).thenReturn(List.of(coupon));
        when(lockRecordMapper.selectList(any())).thenReturn(List.of());
        when(pointsLogMapper.selectList(any())).thenReturn(pointHolds);
        when(walletAccountMapper.selectList(any())).thenReturn(List.of());
        when(tenantMapper.selectBatchIds(any())).thenReturn(List.of(
                tenant(9L, "本地测试店"),
                tenant(10L, "第二测试店")));
        when(salesOrderMapper.selectList(any())).thenReturn(List.of(
                salesOrder("SO1001", 9L, 99L, "PENDING"),
                salesOrder("SO1002", 9L, 99L, "CLOSED")));

        AppAssetSummaryServiceImpl service = new AppAssetSummaryServiceImpl(
                tenantMemberMapper, tenantMapper, walletAccountMapper, pointsAccountMapper, pointsLogMapper,
                growthLogMapper, userCouponMapper, mock(UnifiedWalletLogMapper.class), mock(MerchantWalletLogMapper.class),
                mock(CouponReceiveRecordMapper.class), lockRecordMapper, mock(CouponReleaseRecordMapper.class),
                mock(CouponWriteOffRecordMapper.class), mock(CouponExpireRecordMapper.class), salesOrderMapper);

        List<AssetHoldVO> result = service.listAssetHolds(99L, null);

        assertThat(result).hasSize(100);
        assertThat(result).extracting(AssetHoldVO::getTenantId)
                .contains(9L, 10L);
        assertThat(result).extracting(AssetHoldVO::getTenantName)
                .contains("本地测试店", "第二测试店");
        assertThat(result.get(0).getActionPath()).isEqualTo("/order/SO1001");
        assertThat(result.get(0).getActionLabel()).isEqualTo("查看订单");
    }

    private AppAssetSummaryServiceImpl service(TenantMemberMapper tenantMemberMapper,
                                               TenantMapper tenantMapper,
                                               MerchantWalletAccountMapper walletAccountMapper,
                                               MemberPointsAccountMapper pointsAccountMapper,
                                               MemberPointsLogMapper pointsLogMapper,
                                               MemberGrowthLogMapper growthLogMapper,
                                               UserCouponMapper userCouponMapper) {
        return new AppAssetSummaryServiceImpl(
                tenantMemberMapper,
                tenantMapper,
                walletAccountMapper,
                pointsAccountMapper,
                pointsLogMapper,
                growthLogMapper,
                userCouponMapper,
                mock(UnifiedWalletLogMapper.class),
                mock(MerchantWalletLogMapper.class),
                mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class),
                mock(CouponReleaseRecordMapper.class),
                mock(CouponWriteOffRecordMapper.class),
                mock(CouponExpireRecordMapper.class),
                mock(SalesOrderMapper.class));
    }

    private Map<String, Object> row(Object... values) {
        java.util.LinkedHashMap<String, Object> row = new java.util.LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            row.put(String.valueOf(values[index]), values[index + 1]);
        }
        return row;
    }

    private <T> Page<T> page(T record) {
        return page(List.of(record));
    }

    private <T> Page<T> page(List<T> records) {
        Page<T> page = new Page<>(1, 10);
        page.setRecords(records);
        return page;
    }

    private TenantMember member(Long tenantId, Integer status) {
        TenantMember member = new TenantMember();
        member.setTenantId(tenantId);
        member.setMemberStatus(status);
        return member;
    }

    private MerchantWalletAccount wallet(Long tenantId, String available, String frozen) {
        MerchantWalletAccount wallet = new MerchantWalletAccount();
        wallet.setTenantId(tenantId);
        wallet.setAvailableAmount(new BigDecimal(available));
        wallet.setFrozenAmount(new BigDecimal(frozen));
        return wallet;
    }

    private MemberPointsAccount points(Long tenantId, Integer points) {
        MemberPointsAccount account = new MemberPointsAccount();
        account.setTenantId(tenantId);
        account.setPoints(points);
        return account;
    }

    private MemberPointsLog pointsLog(LocalDateTime createTime, Long tenantId, Integer points) {
        return pointsLog(null, createTime, tenantId, points);
    }

    private MemberPointsLog pointsLog(Long id, LocalDateTime createTime, Long tenantId, Integer points) {
        MemberPointsLog log = new MemberPointsLog();
        log.setId(id);
        log.setTenantId(tenantId);
        log.setChangePoints(points);
        log.setBizType("ORDER_REWARD");
        log.setBizNo("SO1001");
        log.setCreateTime(createTime);
        return log;
    }

    private MemberPointsLog releasedPointsLog(LocalDateTime releaseTime, Long tenantId, Integer points) {
        MemberPointsLog log = pointsLog(releaseTime.minusMinutes(5), tenantId, -points);
        log.setStatus("RELEASED");
        log.setReleaseTime(releaseTime);
        log.setReleaseReason("订单取消");
        return log;
    }

    private MemberGrowthLog growthLog(LocalDateTime createTime, Long tenantId, Integer value) {
        return growthLog(null, createTime, tenantId, value);
    }

    private MemberGrowthLog growthLog(Long id, LocalDateTime createTime, Long tenantId, Integer value) {
        MemberGrowthLog log = new MemberGrowthLog();
        log.setId(id);
        log.setTenantId(tenantId);
        log.setChangeGrowth(value);
        log.setBizType("ORDER_REWARD");
        log.setBizNo("SO1001");
        log.setCreateTime(createTime);
        return log;
    }

    private UnifiedWalletLog unifiedWallet(LocalDateTime createTime, String bizType, String bizNo, String amount) {
        return unifiedWallet(null, createTime, bizType, bizNo, amount);
    }

    private UnifiedWalletLog unifiedWallet(Long id, LocalDateTime createTime, String bizType, String bizNo, String amount) {
        UnifiedWalletLog log = new UnifiedWalletLog();
        log.setId(id);
        log.setPlatformUserId(99L);
        log.setBizType(bizType);
        log.setBizNo(bizNo);
        log.setChangeAmount(new BigDecimal(amount));
        log.setCreateTime(createTime);
        return log;
    }

    private MerchantWalletLog merchantWallet(LocalDateTime createTime, Long tenantId, String bizType, String bizNo, String amount) {
        return merchantWallet(null, createTime, tenantId, bizType, bizNo, amount);
    }

    private MerchantWalletLog merchantWallet(Long id, LocalDateTime createTime, Long tenantId, String bizType, String bizNo, String amount) {
        MerchantWalletLog log = new MerchantWalletLog();
        log.setId(id);
        log.setPlatformUserId(99L);
        log.setTenantId(tenantId);
        log.setBizType(bizType);
        log.setBizNo(bizNo);
        log.setChangeAmount(new BigDecimal(amount));
        log.setCreateTime(createTime);
        return log;
    }

    private UserCoupon userCoupon(Long id) {
        UserCoupon coupon = new UserCoupon();
        coupon.setId(id);
        return coupon;
    }

    private CouponReceiveRecord receiveRecord(LocalDateTime receiveTime, Long tenantId, String bizNo) {
        CouponReceiveRecord record = new CouponReceiveRecord();
        record.setTenantId(tenantId);
        record.setBizNo(bizNo);
        record.setReceiveTime(receiveTime);
        return record;
    }

    private CouponLockRecord lockRecord(LocalDateTime lockTime, Long tenantId, String orderNo) {
        CouponLockRecord record = new CouponLockRecord();
        record.setTenantId(tenantId);
        record.setOrderNo(orderNo);
        record.setLockTime(lockTime);
        return record;
    }

    private Tenant tenant(Long id, String name) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName(name);
        tenant.setDeleted(0);
        tenant.setStatus(1);
        return tenant;
    }

    private SalesOrder salesOrder(String orderNo, Long tenantId, Long platformUserId, String orderStatus) {
        SalesOrder order = new SalesOrder();
        order.setOrderNo(orderNo);
        order.setTenantId(tenantId);
        order.setPlatformUserId(platformUserId);
        order.setOrderStatus(orderStatus);
        return order;
    }
}
