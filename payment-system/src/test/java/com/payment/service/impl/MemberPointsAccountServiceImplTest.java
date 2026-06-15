package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.MemberPointsLog;
import com.payment.enums.PointsDeductStatusEnum;
import com.payment.mapper.MemberPointsAccountMapper;
import com.payment.mapper.MemberPointsLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

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

    @Test
    void grantPointsShouldWriteExpireTimeWhenProvided() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = new MemberPointsAccountServiceImpl(accountMapper, logMapper);
        LocalDateTime expireTime = LocalDateTime.of(2026, 7, 1, 0, 0);

        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 200, 0));
        when(accountMapper.updateById(any())).thenReturn(1);

        service.grantPoints(9L, 100L, 100, "ORDER_REWARD", "SO1002", "消费赠送积分", expireTime);

        ArgumentCaptor<MemberPointsLog> logCaptor = ArgumentCaptor.forClass(MemberPointsLog.class);
        verify(logMapper).insert(logCaptor.capture());
        assertEquals(expireTime, logCaptor.getValue().getExpireTime());
    }

    @Test
    void expirePointsShouldDeductAccountMarkSourceExpiredAndWriteExpireLog() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = new MemberPointsAccountServiceImpl(accountMapper, logMapper);
        LocalDateTime now = LocalDateTime.of(2026, 7, 2, 2, 0);

        when(logMapper.selectList(any())).thenReturn(List.of(earnedLog(10L, 9L, 100L, 300)));
        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 500, 0));
        when(accountMapper.updateById(any())).thenReturn(1);
        when(logMapper.update(any(), any())).thenReturn(1);

        int expiredPoints = service.expirePoints(now, 100);

        assertEquals(300, expiredPoints);

        ArgumentCaptor<MemberPointsAccount> accountCaptor = ArgumentCaptor.forClass(MemberPointsAccount.class);
        verify(accountMapper).updateById(accountCaptor.capture());
        assertEquals(200, accountCaptor.getValue().getPoints());
        assertEquals(300, accountCaptor.getValue().getTotalUsed());

        ArgumentCaptor<MemberPointsLog> updateCaptor = ArgumentCaptor.forClass(MemberPointsLog.class);
        verify(logMapper).update(updateCaptor.capture(), any());
        assertEquals(PointsDeductStatusEnum.EXPIRED.name(), updateCaptor.getValue().getStatus());
        assertEquals(now, updateCaptor.getValue().getReleaseTime());

        ArgumentCaptor<MemberPointsLog> insertCaptor = ArgumentCaptor.forClass(MemberPointsLog.class);
        verify(logMapper).insert(insertCaptor.capture());
        assertEquals("POINTS_EXPIRE", insertCaptor.getValue().getBizType());
        assertEquals("POINTS_EXPIRE_10", insertCaptor.getValue().getBizNo());
        assertEquals(-300, insertCaptor.getValue().getChangePoints());
        assertEquals(500, insertCaptor.getValue().getPointsBefore());
        assertEquals(200, insertCaptor.getValue().getPointsAfter());
        assertEquals(PointsDeductStatusEnum.CONFIRMED.name(), insertCaptor.getValue().getStatus());
    }

    @Test
    void expirePointsShouldOnlyDeductAvailableBalance() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = new MemberPointsAccountServiceImpl(accountMapper, logMapper);

        when(logMapper.selectList(any())).thenReturn(List.of(earnedLog(10L, 9L, 100L, 300)));
        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 120, 0));
        when(accountMapper.updateById(any())).thenReturn(1);
        when(logMapper.update(any(), any())).thenReturn(1);

        int expiredPoints = service.expirePoints(LocalDateTime.now(), 100);

        assertEquals(120, expiredPoints);

        ArgumentCaptor<MemberPointsAccount> accountCaptor = ArgumentCaptor.forClass(MemberPointsAccount.class);
        verify(accountMapper).updateById(accountCaptor.capture());
        assertEquals(0, accountCaptor.getValue().getPoints());

        ArgumentCaptor<MemberPointsLog> insertCaptor = ArgumentCaptor.forClass(MemberPointsLog.class);
        verify(logMapper).insert(insertCaptor.capture());
        assertEquals(-120, insertCaptor.getValue().getChangePoints());
        assertEquals(0, insertCaptor.getValue().getPointsAfter());
    }

    @Test
    void getExpiringPointsShouldCapByCurrentBalance() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = new MemberPointsAccountServiceImpl(accountMapper, logMapper);

        when(logMapper.selectList(any())).thenReturn(List.of(
                earnedLog(10L, 9L, 100L, 300),
                earnedLog(11L, 9L, 100L, 200)
        ));
        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 350, 0));

        Integer expiringPoints = service.getExpiringPoints(
                9L,
                100L,
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 7, 31, 23, 59)
        );

        assertEquals(350, expiringPoints);
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

    private MemberPointsLog earnedLog(Long id, Long tenantId, Long platformUserId, Integer points) {
        MemberPointsLog log = new MemberPointsLog();
        log.setId(id);
        log.setTenantId(tenantId);
        log.setPlatformUserId(platformUserId);
        log.setBizType("ORDER_REWARD");
        log.setBizNo("SO1002");
        log.setChangePoints(points);
        log.setPointsBefore(200);
        log.setPointsAfter(200 + points);
        log.setStatus(PointsDeductStatusEnum.CONFIRMED.name());
        log.setExpireTime(LocalDateTime.of(2026, 7, 1, 0, 0));
        return log;
    }
}
