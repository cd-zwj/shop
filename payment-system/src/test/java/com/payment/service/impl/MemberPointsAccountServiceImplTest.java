package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.config.RabbitMQConfig;
import com.payment.entity.CompensationTask;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.MemberPointsLog;
import com.payment.enums.PointsDeductStatusEnum;
import com.payment.mapper.MemberPointsAccountMapper;
import com.payment.mapper.MemberPointsLogMapper;
import com.payment.service.CompensationTaskFactory;
import com.payment.service.OutboxPublisher;
import com.payment.service.outbox.OutboxMessageCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberPointsAccountServiceImplTest {

    @Test
    void holdPointsShouldDeductAccountAndWritePreHoldLog() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = service(accountMapper, logMapper);

        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 500, 0));
        when(accountMapper.update(isNull(), any())).thenReturn(1);

        MemberPointsLog result = service.holdPoints(9L, 100L, 300, "ORDER_DEDUCT", "SO1001", "订单积分预占");

        verify(accountMapper).update(isNull(), any());
        verify(accountMapper, never()).updateById(any(MemberPointsAccount.class));

        ArgumentCaptor<MemberPointsLog> logCaptor = ArgumentCaptor.forClass(MemberPointsLog.class);
        verify(logMapper).insert(logCaptor.capture());
        assertEquals(-300, logCaptor.getValue().getChangePoints());
        assertEquals(500, logCaptor.getValue().getPointsBefore());
        assertEquals(200, logCaptor.getValue().getPointsAfter());
        assertEquals(PointsDeductStatusEnum.PRE_HOLD.name(), logCaptor.getValue().getStatus());
        assertEquals(PointsDeductStatusEnum.PRE_HOLD.name(), result.getStatus());

        assertPointsEventPublished(service, "POINTS_HOLD", "ORDER_DEDUCT", "SO1001", -300,
                PointsDeductStatusEnum.PRE_HOLD.name());
    }

    @Test
    void holdPointsShouldRejectInsufficientPoints() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = service(accountMapper, logMapper);

        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 100, 0));

        assertThrows(BusinessException.class,
                () -> service.holdPoints(9L, 100L, 300, "ORDER_DEDUCT", "SO1001", "订单积分预占"));
    }

    @Test
    void confirmPointsHoldShouldMarkPreHoldAsConfirmed() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = service(accountMapper, logMapper);

        when(logMapper.selectOne(any())).thenReturn(preHoldLog());

        service.confirmPointsHold(9L, 100L, "ORDER_DEDUCT", "SO1001");

        ArgumentCaptor<MemberPointsLog> logCaptor = ArgumentCaptor.forClass(MemberPointsLog.class);
        verify(logMapper).updateById(logCaptor.capture());
        assertEquals(PointsDeductStatusEnum.CONFIRMED.name(), logCaptor.getValue().getStatus());

        assertPointsEventPublished(service, "POINTS_HOLD_CONFIRMED", "ORDER_DEDUCT", "SO1001", -300,
                PointsDeductStatusEnum.CONFIRMED.name());
    }

    @Test
    void releasePointsHoldShouldRefundAccountAndMarkLogReleased() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = service(accountMapper, logMapper);

        when(logMapper.selectOne(any())).thenReturn(preHoldLog());
        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 200, 300));
        when(accountMapper.update(isNull(), any())).thenReturn(1);

        service.releasePointsHold(9L, 100L, "ORDER_DEDUCT", "SO1001", "订单取消");

        verify(accountMapper).update(isNull(), any());
        verify(accountMapper, never()).updateById(any(MemberPointsAccount.class));

        ArgumentCaptor<MemberPointsLog> logCaptor = ArgumentCaptor.forClass(MemberPointsLog.class);
        verify(logMapper).updateById(logCaptor.capture());
        assertEquals(PointsDeductStatusEnum.RELEASED.name(), logCaptor.getValue().getStatus());
        assertEquals("订单取消", logCaptor.getValue().getReleaseReason());

        assertPointsEventPublished(service, "POINTS_HOLD_RELEASED", "ORDER_DEDUCT", "SO1001", -300,
                PointsDeductStatusEnum.RELEASED.name());
    }

    @Test
    void grantPointsShouldWriteExpireTimeWhenProvided() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = service(accountMapper, logMapper);
        LocalDateTime expireTime = LocalDateTime.of(2026, 7, 1, 0, 0);

        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 200, 0));
        when(accountMapper.update(isNull(), any())).thenReturn(1);

        service.grantPoints(9L, 100L, 100, "ORDER_REWARD", "SO1002", "消费赠送积分", expireTime);

        verify(accountMapper).update(isNull(), any());
        verify(accountMapper, never()).updateById(any(MemberPointsAccount.class));

        ArgumentCaptor<MemberPointsLog> logCaptor = ArgumentCaptor.forClass(MemberPointsLog.class);
        verify(logMapper).insert(logCaptor.capture());
        assertEquals(expireTime, logCaptor.getValue().getExpireTime());
        assertEquals(200, logCaptor.getValue().getPointsBefore());
        assertEquals(300, logCaptor.getValue().getPointsAfter());

        assertPointsEventPublished(service, "POINTS_GRANTED", "ORDER_REWARD", "SO1002", 100,
                PointsDeductStatusEnum.CONFIRMED.name());
    }

    @Test
    void grantPointsShouldUseRetriedBalanceInLogWhenOptimisticLockConflicts() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = service(accountMapper, logMapper);

        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 200, 0));
        when(accountMapper.update(isNull(), any()))
                .thenReturn(0)
                .thenReturn(1);
        when(accountMapper.selectById(1L)).thenReturn(account(9L, 100L, 260, 0));

        service.grantPoints(9L, 100L, 100, "ORDER_REWARD", "SO1002", "消费赠送积分");

        verify(accountMapper, times(2)).update(isNull(), any());
        verify(accountMapper, never()).updateById(any(MemberPointsAccount.class));

        ArgumentCaptor<MemberPointsLog> logCaptor = ArgumentCaptor.forClass(MemberPointsLog.class);
        verify(logMapper).insert(logCaptor.capture());
        assertEquals(260, logCaptor.getValue().getPointsBefore());
        assertEquals(360, logCaptor.getValue().getPointsAfter());
    }

    @Test
    void holdPointsShouldRecheckBalanceAfterOptimisticLockConflict() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = service(accountMapper, logMapper);

        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 500, 0));
        when(accountMapper.update(isNull(), any())).thenReturn(0);
        when(accountMapper.selectById(1L)).thenReturn(account(9L, 100L, 80, 0));

        assertThrows(BusinessException.class,
                () -> service.holdPoints(9L, 100L, 300, "ORDER_DEDUCT", "SO1001", "订单积分预占"));

        verify(accountMapper, times(1)).update(isNull(), any());
        verify(accountMapper, never()).updateById(any(MemberPointsAccount.class));
    }

    @Test
    void expirePointsShouldDeductAccountMarkSourceExpiredAndWriteExpireLog() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = service(accountMapper, logMapper);
        LocalDateTime now = LocalDateTime.of(2026, 7, 2, 2, 0);

        when(logMapper.selectList(any())).thenReturn(List.of(earnedLog(10L, 9L, 100L, 300)));
        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 500, 0));
        when(accountMapper.update(isNull(), any())).thenReturn(1);
        when(logMapper.update(any(), any())).thenReturn(1);

        int expiredPoints = service.expirePoints(now, 100);

        assertEquals(300, expiredPoints);

        verify(accountMapper).update(isNull(), any());
        verify(accountMapper, never()).updateById(any(MemberPointsAccount.class));

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

        assertPointsEventPublished(service, "POINTS_EXPIRED", "POINTS_EXPIRE", "POINTS_EXPIRE_10", -300,
                PointsDeductStatusEnum.CONFIRMED.name());
    }

    @Test
    void expirePointsShouldOnlyDeductAvailableBalance() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = service(accountMapper, logMapper);

        when(logMapper.selectList(any())).thenReturn(List.of(earnedLog(10L, 9L, 100L, 300)));
        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 120, 0));
        when(accountMapper.update(isNull(), any())).thenReturn(1);
        when(logMapper.update(any(), any())).thenReturn(1);

        int expiredPoints = service.expirePoints(LocalDateTime.now(), 100);

        assertEquals(120, expiredPoints);

        verify(accountMapper).update(isNull(), any());
        verify(accountMapper, never()).updateById(any(MemberPointsAccount.class));

        ArgumentCaptor<MemberPointsLog> insertCaptor = ArgumentCaptor.forClass(MemberPointsLog.class);
        verify(logMapper).insert(insertCaptor.capture());
        assertEquals(-120, insertCaptor.getValue().getChangePoints());
        assertEquals(0, insertCaptor.getValue().getPointsAfter());

        assertPointsEventPublished(service, "POINTS_EXPIRED", "POINTS_EXPIRE", "POINTS_EXPIRE_10", -120,
                PointsDeductStatusEnum.CONFIRMED.name());
    }

    @Test
    void getExpiringPointsShouldCapByCurrentBalance() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsAccountServiceImpl service = service(accountMapper, logMapper);

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

    @Test
    void grantPointsShouldCreateCompensationTaskWhenAccountUpdateRetriesExhausted() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        CompensationTaskFactory compensationTaskFactory = mock(CompensationTaskFactory.class);
        MemberPointsAccountServiceImpl service = service(accountMapper, logMapper, compensationTaskFactory);

        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 200, 0));
        when(accountMapper.selectById(1L)).thenReturn(account(9L, 100L, 200, 0));
        when(accountMapper.update(isNull(), any())).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.grantPoints(9L, 100L, 100, "ORDER_REWARD", "SO1002", "消费赠送积分"));

        verify(compensationTaskFactory).createIfAbsent("POINTS_GRANT", "SO1002", "积分发放失败，请重试");
    }

    @Test
    void expirePointsShouldCreateCompensationTaskWhenAccountUpdateRetriesExhausted() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        CompensationTaskFactory compensationTaskFactory = mock(CompensationTaskFactory.class);
        MemberPointsAccountServiceImpl service = service(accountMapper, logMapper, compensationTaskFactory);

        when(logMapper.selectList(any())).thenReturn(List.of(earnedLog(10L, 9L, 100L, 300)));
        when(logMapper.update(any(), any())).thenReturn(1);
        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 500, 0));
        when(accountMapper.selectById(1L)).thenReturn(account(9L, 100L, 500, 0));
        when(accountMapper.update(isNull(), any())).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.expirePoints(LocalDateTime.of(2026, 7, 2, 2, 0), 100));

        verify(compensationTaskFactory).createIfAbsent("POINTS_EXPIRE", "POINTS_EXPIRE_10", "积分过期扣减失败，请重试");
    }

    @Test
    void holdPointsShouldCreateCompensationTaskWhenAccountUpdateRetriesExhausted() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        CompensationTaskFactory compensationTaskFactory = mock(CompensationTaskFactory.class);
        MemberPointsAccountServiceImpl service = service(accountMapper, logMapper, compensationTaskFactory);

        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 500, 0));
        when(accountMapper.selectById(1L)).thenReturn(account(9L, 100L, 500, 0));
        when(accountMapper.update(isNull(), any())).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.holdPoints(9L, 100L, 100, "ORDER_DEDUCT", "SO1003", "订单积分预占"));

        verify(compensationTaskFactory).createIfAbsent("POINTS_HOLD", "SO1003", "积分预占失败，请重试");
    }

    @Test
    void releasePointsHoldShouldCreateCompensationTaskWhenAccountUpdateRetriesExhausted() {
        MemberPointsAccountMapper accountMapper = mock(MemberPointsAccountMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        CompensationTaskFactory compensationTaskFactory = mock(CompensationTaskFactory.class);
        MemberPointsAccountServiceImpl service = service(accountMapper, logMapper, compensationTaskFactory);

        when(logMapper.selectOne(any())).thenReturn(preHoldLog());
        when(accountMapper.selectOne(any())).thenReturn(account(9L, 100L, 200, 300));
        when(accountMapper.selectById(1L)).thenReturn(account(9L, 100L, 200, 300));
        when(accountMapper.update(isNull(), any())).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.releasePointsHold(9L, 100L, "ORDER_DEDUCT", "SO1001", "订单取消"));

        verify(compensationTaskFactory).createIfAbsent("POINTS_RELEASE", "SO1001", "积分释放失败，请重试");
    }

    private MemberPointsAccountServiceImpl service(MemberPointsAccountMapper accountMapper,
                                                   MemberPointsLogMapper logMapper) {
        return service(accountMapper, logMapper, mock(CompensationTaskFactory.class));
    }

    private MemberPointsAccountServiceImpl service(MemberPointsAccountMapper accountMapper,
                                                   MemberPointsLogMapper logMapper,
                                                   CompensationTaskFactory compensationTaskFactory) {
        return service(accountMapper, logMapper, compensationTaskFactory, mock(OutboxPublisher.class));
    }

    private MemberPointsAccountServiceImpl service(MemberPointsAccountMapper accountMapper,
                                                   MemberPointsLogMapper logMapper,
                                                   CompensationTaskFactory compensationTaskFactory,
                                                   OutboxPublisher outboxPublisher) {
        when(compensationTaskFactory.createIfAbsent(any(), any(), any())).thenReturn(new CompensationTask());
        MemberPointsAccountServiceImpl service = new MemberPointsAccountServiceImpl(
                accountMapper,
                logMapper,
                compensationTaskFactory,
                outboxPublisher);
        serviceTestOutbox(service, outboxPublisher);
        return service;
    }

    private void assertPointsEventPublished(MemberPointsAccountServiceImpl service,
                                            String eventType,
                                            String bizType,
                                            String bizNo,
                                            Integer changePoints,
                                            String status) {
        OutboxPublisher outboxPublisher = serviceTestOutbox(service, null);
        ArgumentCaptor<OutboxMessageCommand> commandCaptor = ArgumentCaptor.forClass(OutboxMessageCommand.class);
        verify(outboxPublisher).publish(commandCaptor.capture());
        OutboxMessageCommand command = commandCaptor.getValue();
        assertEquals("POINTS_EVENT", command.getBizType());
        assertEquals(bizNo, command.getBizNo());
        assertEquals(RabbitMQConfig.POINTS_EVENT_QUEUE, command.getRoutingKey());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) command.getMessageBody();
        assertEquals(eventType, body.get("eventType"));
        assertEquals(bizType, body.get("bizType"));
        assertEquals(bizNo, body.get("bizNo"));
        assertEquals(changePoints, body.get("changePoints"));
        assertEquals(status, body.get("status"));
    }

    private OutboxPublisher serviceTestOutbox(MemberPointsAccountServiceImpl service, OutboxPublisher outboxPublisher) {
        if (outboxPublisher != null) {
            TestOutboxRegistry.OUTBOX.put(service, outboxPublisher);
        }
        return TestOutboxRegistry.OUTBOX.get(service);
    }

    private static class TestOutboxRegistry {
        private static final java.util.Map<MemberPointsAccountServiceImpl, OutboxPublisher> OUTBOX =
                new java.util.IdentityHashMap<>();
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
