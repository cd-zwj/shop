package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.payment.common.BusinessException;
import com.payment.entity.MerchantBalance;
import com.payment.entity.Withdrawal;
import com.payment.mapper.MerchantBalanceMapper;
import com.payment.mapper.TenantEmployeeMapper;
import com.payment.mapper.TenantMapper;
import com.payment.mapper.UserMapper;
import com.payment.mapper.WithdrawalMapper;
import com.payment.service.UserNotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WithdrawalServiceImplTest {

    @Test
    void approveWithdrawalShouldNotMoveMoneyWhenStatusClaimFails() {
        WithdrawalMapper withdrawalMapper = mock(WithdrawalMapper.class);
        MerchantBalanceMapper merchantBalanceMapper = mock(MerchantBalanceMapper.class);
        WithdrawalServiceImpl service = service(withdrawalMapper, merchantBalanceMapper);

        when(withdrawalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pendingWithdrawal());
        when(merchantBalanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(balanceWithFrozen("100.00"));
        when(withdrawalMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.approveWithdrawal(1L));

        verify(merchantBalanceMapper, never()).updateById(any(MerchantBalance.class));
    }

    @Test
    void approveWithdrawalShouldClaimStatusBeforeMovingFrozenBalance() {
        WithdrawalMapper withdrawalMapper = mock(WithdrawalMapper.class);
        MerchantBalanceMapper merchantBalanceMapper = mock(MerchantBalanceMapper.class);
        WithdrawalServiceImpl service = service(withdrawalMapper, merchantBalanceMapper);

        when(withdrawalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pendingWithdrawal());
        when(merchantBalanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(balanceWithFrozen("100.00"));
        when(withdrawalMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(merchantBalanceMapper.updateById(any(MerchantBalance.class))).thenReturn(1);

        service.approveWithdrawal(1L);

        InOrder inOrder = inOrder(withdrawalMapper, merchantBalanceMapper);
        inOrder.verify(withdrawalMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        inOrder.verify(merchantBalanceMapper).updateById(any(MerchantBalance.class));
    }

    @Test
    void rejectWithdrawalShouldFailBeforeStatusChangeWhenFrozenBalanceInsufficient() {
        WithdrawalMapper withdrawalMapper = mock(WithdrawalMapper.class);
        MerchantBalanceMapper merchantBalanceMapper = mock(MerchantBalanceMapper.class);
        WithdrawalServiceImpl service = service(withdrawalMapper, merchantBalanceMapper);

        when(withdrawalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pendingWithdrawal());
        when(merchantBalanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(balanceWithFrozen("5.00"));

        assertThrows(BusinessException.class, () -> service.rejectWithdrawal(1L, "资料不完整"));

        verify(withdrawalMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(merchantBalanceMapper, never()).updateById(any(MerchantBalance.class));
    }

    @Test
    void rejectWithdrawalShouldClaimStatusBeforeUnfreezingBalance() {
        WithdrawalMapper withdrawalMapper = mock(WithdrawalMapper.class);
        MerchantBalanceMapper merchantBalanceMapper = mock(MerchantBalanceMapper.class);
        WithdrawalServiceImpl service = service(withdrawalMapper, merchantBalanceMapper);

        when(withdrawalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pendingWithdrawal());
        when(merchantBalanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(balanceWithFrozen("100.00"));
        when(withdrawalMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(merchantBalanceMapper.updateById(any(MerchantBalance.class))).thenReturn(1);

        service.rejectWithdrawal(1L, "资料不完整");

        InOrder inOrder = inOrder(withdrawalMapper, merchantBalanceMapper);
        inOrder.verify(withdrawalMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        inOrder.verify(merchantBalanceMapper).updateById(any(MerchantBalance.class));
    }

    @Test
    void rejectWithdrawalShouldThrowWhenUnfreezeRetriesExhausted() {
        WithdrawalMapper withdrawalMapper = mock(WithdrawalMapper.class);
        MerchantBalanceMapper merchantBalanceMapper = mock(MerchantBalanceMapper.class);
        WithdrawalServiceImpl service = service(withdrawalMapper, merchantBalanceMapper);

        when(withdrawalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pendingWithdrawal());
        when(merchantBalanceMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(balanceWithFrozen("100.00"))
                .thenReturn(balanceWithFrozen("100.00"))
                .thenReturn(balanceWithFrozen("100.00"))
                .thenReturn(balanceWithFrozen("100.00"));
        when(withdrawalMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(merchantBalanceMapper.updateById(any(MerchantBalance.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.rejectWithdrawal(1L, "资料不完整"));
    }

    private WithdrawalServiceImpl service(WithdrawalMapper withdrawalMapper, MerchantBalanceMapper merchantBalanceMapper) {
        WithdrawalServiceImpl service = new WithdrawalServiceImpl();
        ReflectionTestUtils.setField(service, "withdrawalMapper", withdrawalMapper);
        ReflectionTestUtils.setField(service, "merchantBalanceMapper", merchantBalanceMapper);
        ReflectionTestUtils.setField(service, "tenantMapper", mock(TenantMapper.class));
        ReflectionTestUtils.setField(service, "userMapper", mock(UserMapper.class));
        ReflectionTestUtils.setField(service, "tenantEmployeeMapper", mock(TenantEmployeeMapper.class));
        ReflectionTestUtils.setField(service, "notificationService", mock(UserNotificationService.class));
        return service;
    }

    private Withdrawal pendingWithdrawal() {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setId(1L);
        withdrawal.setTenantId(9L);
        withdrawal.setAmount(new BigDecimal("10.00"));
        withdrawal.setStatus(0);
        withdrawal.setDeleted(0);
        return withdrawal;
    }

    private MerchantBalance balanceWithFrozen(String frozenBalance) {
        MerchantBalance balance = new MerchantBalance();
        balance.setId(1L);
        balance.setTenantId(9L);
        balance.setBalance(new BigDecimal("50.00"));
        balance.setFrozenBalance(new BigDecimal(frozenBalance));
        balance.setTotalIncome(new BigDecimal("100.00"));
        balance.setTotalWithdrawal(BigDecimal.ZERO);
        balance.setDeleted(0);
        balance.setVersion(1);
        return balance;
    }
}
