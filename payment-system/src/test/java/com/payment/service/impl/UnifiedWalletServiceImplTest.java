package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.entity.UnifiedWalletAccount;
import com.payment.entity.UnifiedWalletLog;
import com.payment.mapper.UnifiedWalletAccountMapper;
import com.payment.mapper.UnifiedWalletLogMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnifiedWalletServiceImplTest {

    @Test
    void creditShouldRetryWhenOptimisticLockConflicts() {
        UnifiedWalletAccountMapper accountMapper = mock(UnifiedWalletAccountMapper.class);
        UnifiedWalletLogMapper logMapper = mock(UnifiedWalletLogMapper.class);
        UnifiedWalletServiceImpl service = new UnifiedWalletServiceImpl(accountMapper, logMapper);

        when(accountMapper.selectOne(any()))
                .thenReturn(buildUnifiedAccount(1L, "100.00"))
                .thenReturn(buildUnifiedAccount(1L, "100.00"));
        when(accountMapper.update(isNull(), any()))
                .thenReturn(0)
                .thenReturn(1);

        assertDoesNotThrow(() -> service.credit(1L, new BigDecimal("10.00"), "TEST", "BIZ-1", "retry"));
        verify(accountMapper, times(2)).update(isNull(), any());
        verify(accountMapper, never()).updateById(any(UnifiedWalletAccount.class));
        verify(logMapper, times(1)).insert(any(UnifiedWalletLog.class));
    }

    @Test
    void debitShouldFailWhenBalanceBecomesInsufficientAfterConflict() {
        UnifiedWalletAccountMapper accountMapper = mock(UnifiedWalletAccountMapper.class);
        UnifiedWalletLogMapper logMapper = mock(UnifiedWalletLogMapper.class);
        UnifiedWalletServiceImpl service = new UnifiedWalletServiceImpl(accountMapper, logMapper);

        when(accountMapper.selectOne(any()))
                .thenReturn(buildUnifiedAccount(1L, "100.00"))
                .thenReturn(buildUnifiedAccount(1L, "10.00"));
        when(accountMapper.update(isNull(), any())).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.debit(1L, new BigDecimal("80.00"), "TEST", "BIZ-2", "debit"));
        verify(accountMapper, times(1)).update(isNull(), any());
        verify(accountMapper, never()).updateById(any(UnifiedWalletAccount.class));
    }

    @Test
    void refundCreditShouldNotIncreaseTotalRecharge() {
        UnifiedWalletAccountMapper accountMapper = mock(UnifiedWalletAccountMapper.class);
        UnifiedWalletLogMapper logMapper = mock(UnifiedWalletLogMapper.class);
        UnifiedWalletServiceImpl service = new UnifiedWalletServiceImpl(accountMapper, logMapper);
        when(accountMapper.selectOne(any())).thenReturn(buildUnifiedAccount(1L, "100.00"));
        when(accountMapper.update(isNull(), any())).thenReturn(1);

        service.credit(1L, new BigDecimal("10.00"),
                "ORDER_PAYMENT_FAILED_REFUND", "SO-1", "支付失败回退");

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<LambdaUpdateWrapper> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(accountMapper).update(isNull(), captor.capture());
        assertFalse(captor.getValue().getSqlSet().contains("total_recharge"));
    }

    private UnifiedWalletAccount buildUnifiedAccount(Long platformUserId, String balance) {
        UnifiedWalletAccount account = new UnifiedWalletAccount();
        account.setId(1L);
        account.setPlatformUserId(platformUserId);
        account.setAvailableAmount(new BigDecimal(balance));
        account.setFrozenAmount(BigDecimal.ZERO);
        account.setTotalRecharge(BigDecimal.ZERO);
        account.setTotalConsume(BigDecimal.ZERO);
        account.setVersion(0);
        account.setStatus(1);
        return account;
    }
}
