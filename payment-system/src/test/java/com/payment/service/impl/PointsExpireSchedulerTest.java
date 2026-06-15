package com.payment.service.impl;

import com.payment.service.MemberPointsAccountService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PointsExpireSchedulerTest {

    @Test
    void expirePointsShouldDelegateToMemberPointsService() {
        MemberPointsAccountService memberPointsAccountService = mock(MemberPointsAccountService.class);
        PointsExpireScheduler scheduler = new PointsExpireScheduler(memberPointsAccountService);

        scheduler.expirePoints();

        verify(memberPointsAccountService).expirePoints(any(), eq(200));
    }
}
