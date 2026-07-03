package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.entity.UnifiedWalletAccount;
import com.payment.entity.UnifiedWalletLog;
import com.payment.mapper.UnifiedWalletAccountMapper;
import com.payment.mapper.UnifiedWalletLogMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
        when(accountMapper.updateById(any(UnifiedWalletAccount.class)))
                .thenReturn(0)
                .thenReturn(1);

        assertDoesNotThrow(() -> service.credit(1L, new BigDecimal("10.00"), "TEST", "BIZ-1", "retry"));
        verify(accountMapper, times(2)).updateById(any(UnifiedWalletAccount.class));
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
        when(accountMapper.updateById(any(UnifiedWalletAccount.class))).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.debit(1L, new BigDecimal("80.00"), "TEST", "BIZ-2", "debit"));
        verify(accountMapper, times(1)).updateById(any(UnifiedWalletAccount.class));
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
