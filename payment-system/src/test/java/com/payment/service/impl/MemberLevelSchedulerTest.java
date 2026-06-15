package com.payment.service.impl;

import com.payment.entity.TenantMember;
import com.payment.mapper.TenantMemberMapper;
import com.payment.service.MemberGrowthService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberLevelSchedulerTest {

    @Test
    void adjustMemberLevelsShouldDelegateEveryActiveMember() {
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        MemberGrowthService memberGrowthService = mock(MemberGrowthService.class);
        MemberLevelScheduler scheduler = new MemberLevelScheduler(tenantMemberMapper, memberGrowthService);

        TenantMember first = member(9L, 200L);
        TenantMember second = member(10L, 300L);
        when(tenantMemberMapper.selectList(any())).thenReturn(List.of(first, second));

        scheduler.adjustMemberLevels();

        verify(memberGrowthService).checkAndAdjustLevel(200L, 9L);
        verify(memberGrowthService).checkAndAdjustLevel(300L, 10L);
    }

    @Test
    void adjustMemberLevelsShouldContinueWhenOneMemberFails() {
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        MemberGrowthService memberGrowthService = mock(MemberGrowthService.class);
        MemberLevelScheduler scheduler = new MemberLevelScheduler(tenantMemberMapper, memberGrowthService);

        TenantMember first = member(9L, 200L);
        TenantMember second = member(10L, 300L);
        when(tenantMemberMapper.selectList(any())).thenReturn(List.of(first, second));
        doThrow(new RuntimeException("boom")).when(memberGrowthService).checkAndAdjustLevel(200L, 9L);

        scheduler.adjustMemberLevels();

        verify(memberGrowthService).checkAndAdjustLevel(200L, 9L);
        verify(memberGrowthService).checkAndAdjustLevel(300L, 10L);
    }

    private TenantMember member(Long tenantId, Long platformUserId) {
        TenantMember member = new TenantMember();
        member.setTenantId(tenantId);
        member.setPlatformUserId(platformUserId);
        member.setMemberStatus(1);
        member.setMemberLevel(1);
        return member;
    }
}
