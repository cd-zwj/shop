package com.payment.service.impl;

import com.payment.dto.AppTenantAssetSummaryVO;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.MemberPointsLog;
import com.payment.entity.MemberGrowthLog;
import com.payment.entity.MerchantWalletAccount;
import com.payment.entity.Tenant;
import com.payment.entity.TenantMember;
import com.payment.mapper.MemberGrowthLogMapper;
import com.payment.mapper.MemberPointsAccountMapper;
import com.payment.mapper.MemberPointsLogMapper;
import com.payment.mapper.MerchantWalletAccountMapper;
import com.payment.mapper.TenantMapper;
import com.payment.mapper.TenantMemberMapper;
import com.payment.mapper.UserCouponMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
        when(pointsLogMapper.selectList(any()))
                .thenReturn(List.of(expiringLog(1L, 50)))
                .thenReturn(List.of(expiringLog(2L, 200)))
                .thenReturn(List.of());
        when(tenantMapper.selectBatchIds(any())).thenReturn(List.of(
                tenant(1L, "咖啡店"),
                tenant(2L, "书店"),
                tenant(3L, "花店")
        ));
        when(userCouponMapper.selectCount(any()))
                .thenReturn(2L, 0L, 1L, 0L)
                .thenReturn(3L, 1L, 2L, 1L)
                .thenReturn(0L, 0L, 0L, 0L);
        when(growthLogMapper.selectOne(any()))
                .thenReturn(growth(260))
                .thenReturn(growth(80))
                .thenReturn(growth(0));

        AppAssetSummaryServiceImpl service = new AppAssetSummaryServiceImpl(
                tenantMemberMapper,
                tenantMapper,
                walletAccountMapper,
                pointsAccountMapper,
                pointsLogMapper,
                growthLogMapper,
                userCouponMapper);

        List<AppTenantAssetSummaryVO> result = service.listTenantAssetSummaries(99L);

        assertThat(result).extracting(AppTenantAssetSummaryVO::getTenantId)
                .containsExactly(1L, 2L, 3L);
        assertThat(result.get(0).getTenantName()).isEqualTo("咖啡店");
        assertThat(result.get(0).getPoints()).isEqualTo(120);
        assertThat(result.get(0).getExpiringSoonPoints()).isEqualTo(50);
        assertThat(result.get(0).getMemberStatus()).isEqualTo(1);
        assertThat(result.get(0).getUsableCouponCount()).isEqualTo(2);
        assertThat(result.get(0).getUsedCouponCount()).isEqualTo(1);
        assertThat(result.get(0).getTotalGrowth()).isEqualTo(260);
        assertThat(result.get(1).getWalletAvailableAmount()).isEqualByComparingTo("12.50");
        assertThat(result.get(1).getWalletFrozenAmount()).isEqualByComparingTo("1.00");
        assertThat(result.get(1).getPoints()).isEqualTo(80);
        assertThat(result.get(1).getExpiringSoonPoints()).isEqualTo(80);
        assertThat(result.get(1).getUsableCouponCount()).isEqualTo(3);
        assertThat(result.get(1).getLockedCouponCount()).isEqualTo(1);
        assertThat(result.get(1).getTotalGrowth()).isEqualTo(80);
        assertThat(result.get(2).getPoints()).isZero();
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

        AppAssetSummaryServiceImpl service = new AppAssetSummaryServiceImpl(
                tenantMemberMapper,
                tenantMapper,
                walletAccountMapper,
                pointsAccountMapper,
                pointsLogMapper,
                growthLogMapper,
                userCouponMapper);

        assertThat(service.listTenantAssetSummaries(99L)).isEmpty();
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

    private MemberPointsLog expiringLog(Long tenantId, Integer points) {
        MemberPointsLog log = new MemberPointsLog();
        log.setTenantId(tenantId);
        log.setChangePoints(points);
        log.setExpireTime(LocalDateTime.now().plusDays(7));
        return log;
    }

    private MemberGrowthLog growth(Integer value) {
        MemberGrowthLog log = new MemberGrowthLog();
        log.setChangeGrowth(value);
        return log;
    }

    private Tenant tenant(Long id, String name) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName(name);
        tenant.setDeleted(0);
        tenant.setStatus(1);
        return tenant;
    }
}
