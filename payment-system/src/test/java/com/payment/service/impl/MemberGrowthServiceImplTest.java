package com.payment.service.impl;

import com.payment.entity.MemberGrowthLog;
import com.payment.entity.MemberLevel;
import com.payment.entity.TenantMember;
import com.payment.mapper.MemberGrowthLogMapper;
import com.payment.mapper.MemberLevelMapper;
import com.payment.mapper.TenantMemberMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberGrowthServiceImplTest {

    @Test
    void deductGrowthShouldDowngradeWhenTotalFallsBelowDowngradeThreshold() {
        MemberGrowthLogMapper growthLogMapper = mock(MemberGrowthLogMapper.class);
        MemberLevelMapper levelMapper = mock(MemberLevelMapper.class);
        TenantMemberMapper memberMapper = mock(TenantMemberMapper.class);
        MemberGrowthServiceImpl service = new MemberGrowthServiceImpl(growthLogMapper, levelMapper, memberMapper);

        when(growthLogMapper.selectOne(any())).thenReturn(growth(2500), growth(1200));
        when(memberMapper.selectOne(any())).thenReturn(member(3));
        when(levelMapper.selectList(any())).thenReturn(levels());

        service.deductGrowth(200L, 9L, 1300, "MANUAL", "ADJUST-1", "人工扣减");

        ArgumentCaptor<TenantMember> captor = ArgumentCaptor.forClass(TenantMember.class);
        verify(growthLogMapper).insert(any(MemberGrowthLog.class));
        verify(memberMapper).updateById(captor.capture());
        assertEquals(2, captor.getValue().getMemberLevel());
    }

    @Test
    void checkAndAdjustShouldKeepCurrentLevelWhenGrowthAboveDowngradeThreshold() {
        MemberGrowthLogMapper growthLogMapper = mock(MemberGrowthLogMapper.class);
        MemberLevelMapper levelMapper = mock(MemberLevelMapper.class);
        TenantMemberMapper memberMapper = mock(TenantMemberMapper.class);
        MemberGrowthServiceImpl service = new MemberGrowthServiceImpl(growthLogMapper, levelMapper, memberMapper);

        when(growthLogMapper.selectOne(any())).thenReturn(growth(1800));
        when(memberMapper.selectOne(any())).thenReturn(member(3));
        when(levelMapper.selectList(any())).thenReturn(levels());

        service.checkAndAdjustLevel(200L, 9L);

        verify(memberMapper, never()).updateById(any());
    }

    @Test
    void checkAndAdjustShouldUpgradeToHighestQualifiedLevelRank() {
        MemberGrowthLogMapper growthLogMapper = mock(MemberGrowthLogMapper.class);
        MemberLevelMapper levelMapper = mock(MemberLevelMapper.class);
        TenantMemberMapper memberMapper = mock(TenantMemberMapper.class);
        MemberGrowthServiceImpl service = new MemberGrowthServiceImpl(growthLogMapper, levelMapper, memberMapper);

        when(growthLogMapper.selectOne(any())).thenReturn(growth(2500));
        when(memberMapper.selectOne(any())).thenReturn(member(1));
        when(levelMapper.selectList(any())).thenReturn(levels());

        service.checkAndAdjustLevel(200L, 9L);

        ArgumentCaptor<TenantMember> captor = ArgumentCaptor.forClass(TenantMember.class);
        verify(memberMapper).updateById(captor.capture());
        assertEquals(3, captor.getValue().getMemberLevel());
    }

    @Test
    void expiredLevelValidityShouldRecalculateByUpgradeGrowth() {
        MemberGrowthLogMapper growthLogMapper = mock(MemberGrowthLogMapper.class);
        MemberLevelMapper levelMapper = mock(MemberLevelMapper.class);
        TenantMemberMapper memberMapper = mock(TenantMemberMapper.class);
        MemberGrowthServiceImpl service = new MemberGrowthServiceImpl(growthLogMapper, levelMapper, memberMapper);

        TenantMember member = member(3);
        member.setUpdateTime(LocalDateTime.now().minusDays(2));
        List<MemberLevel> levels = List.of(
                level(1L, 1, 0, 0, null),
                level(2L, 2, 500, 400, null),
                level(3L, 3, 2000, 1500, 1)
        );
        when(growthLogMapper.selectOne(any())).thenReturn(growth(1800));
        when(memberMapper.selectOne(any())).thenReturn(member);
        when(levelMapper.selectList(any())).thenReturn(levels);

        service.checkAndAdjustLevel(200L, 9L);

        ArgumentCaptor<TenantMember> captor = ArgumentCaptor.forClass(TenantMember.class);
        verify(memberMapper).updateById(captor.capture());
        assertEquals(2, captor.getValue().getMemberLevel());
    }

    private MemberGrowthLog growth(int totalGrowth) {
        MemberGrowthLog log = new MemberGrowthLog();
        log.setChangeGrowth(totalGrowth);
        return log;
    }

    private TenantMember member(Integer levelRank) {
        TenantMember member = new TenantMember();
        member.setId(100L);
        member.setTenantId(9L);
        member.setPlatformUserId(200L);
        member.setMemberLevel(levelRank);
        member.setMemberStatus(1);
        return member;
    }

    private List<MemberLevel> levels() {
        return List.of(
                level(1L, 1, 0, 0, null),
                level(2L, 2, 500, 400, null),
                level(3L, 3, 2000, 1500, null)
        );
    }

    private MemberLevel level(Long id,
                              Integer rank,
                              Integer upgradeGrowth,
                              Integer downgradeGrowth,
                              Integer levelValidityDays) {
        MemberLevel level = new MemberLevel();
        level.setId(id);
        level.setTenantId(9L);
        level.setLevelRank(rank);
        level.setLevelName("LV" + rank);
        level.setUpgradeGrowth(upgradeGrowth);
        level.setDowngradeGrowth(downgradeGrowth);
        level.setLevelValidityDays(levelValidityDays);
        level.setStatus(1);
        return level;
    }
}
