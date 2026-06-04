package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.MemberPointsLog;
import com.payment.enums.PointsDeductStatusEnum;
import com.payment.mapper.MemberPointsAccountMapper;
import com.payment.mapper.MemberPointsLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberPointsAccountServiceImplTest {

    @Test
    void holdPointsShouldDeductAccountAndWritePreHoldLog() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = new MemberPointsAccountServiceImpl(accountMapper, logMapper);

        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 500, 0));
        when(accountMapper.updateById(any())).thenReturn(1);

        MemberPointsLog result = service.holdPoints(9L, 100L, 300, "ORDER_DEDUCT", "SO1001", "订单积分预占");

        ArgumentCaptor<MemberPointsAccount> accountCaptor = ArgumentCaptor.forClass(MemberPointsAccount.class);
        verify(accountMapper).updateById(accountCaptor.capture());
        assertEquals(200, accountCaptor.getValue().getPoints());
        assertEquals(300, accountCaptor.getValue().getTotalUsed());

        ArgumentCaptor<MemberPointsLog> logCaptor = ArgumentCaptor.forClass(MemberPointsLog.class);
        verify(logMapper).insert(logCaptor.capture());
        assertEquals(-300, logCaptor.getValue().getChangePoints());
        assertEquals(PointsDeductStatusEnum.PRE_HOLD.name(), logCaptor.getValue().getStatus());
        assertEquals(PointsDeductStatusEnum.PRE_HOLD.name(), result.getStatus());
    }

    @Test
    void holdPointsShouldRejectInsufficientPoints() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = new MemberPointsAccountServiceImpl(accountMapper, logMapper);

        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 100, 0));

        assertThrows(BusinessException.class,
                () -> service.holdPoints(9L, 100L, 300, "ORDER_DEDUCT", "SO1001", "订单积分预占"));
    }

    @Test
    void confirmPointsHoldShouldMarkPreHoldAsConfirmed() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = new MemberPointsAccountServiceImpl(accountMapper, logMapper);

        when(logMapper.selectOne(any())).thenReturn(preHoldLog());

        service.confirmPointsHold(9L, 100L, "ORDER_DEDUCT", "SO1001");

        ArgumentCaptor<MemberPointsLog> logCaptor = ArgumentCaptor.forClass(MemberPointsLog.class);
        verify(logMapper).updateById(logCaptor.capture());
        assertEquals(PointsDeductStatusEnum.CONFIRMED.name(), logCaptor.getValue().getStatus());
    }

    @Test
    void releasePointsHoldShouldRefundAccountAndMarkLogReleased() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = new MemberPointsAccountServiceImpl(accountMapper, logMapper);

        when(logMapper.selectOne(any())).thenReturn(preHoldLog());
        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 200, 300));
        when(accountMapper.updateById(any())).thenReturn(1);

        service.releasePointsHold(9L, 100L, "ORDER_DEDUCT", "SO1001", "订单取消");

        ArgumentCaptor<MemberPointsAccount> accountCaptor = ArgumentCaptor.forClass(MemberPointsAccount.class);
        verify(accountMapper).updateById(accountCaptor.capture());
        assertEquals(500, accountCaptor.getValue().getPoints());
        assertEquals(0, accountCaptor.getValue().getTotalUsed());

        ArgumentCaptor<MemberPointsLog> logCaptor = ArgumentCaptor.forClass(MemberPointsLog.class);
        verify(logMapper).updateById(logCaptor.capture());
        assertEquals(PointsDeductStatusEnum.RELEASED.name(), logCaptor.getValue().getStatus());
        assertEquals("订单取消", logCaptor.getValue().getReleaseReason());
    }

    private MemberPointsAccount account(Long tenantId, Long platformUserId, Integer points, Integer totalUsed) {
        MemberPointsAccount account = new MemberPointsAccount();
        account.setId(1L);
        account.setTenantId(tenantId);
        account.setPlatformUserId(platformUserId);
        account.setPoints(points);
        account.setTotalEarned(0);
        account.setTotalUsed(totalUsed);
        account.setVersion(0);
        account.setStatus(1);
        return account;
    }

    private MemberPointsLog preHoldLog() {
        MemberPointsLog log = new MemberPointsLog();
        log.setId(10L);
        log.setTenantId(9L);
        log.setPlatformUserId(100L);
        log.setBizType("ORDER_DEDUCT");
        log.setBizNo("SO1001");
        log.setChangePoints(-300);
        log.setPointsBefore(500);
        log.setPointsAfter(200);
        log.setStatus(PointsDeductStatusEnum.PRE_HOLD.name());
        return log;
    }
}
