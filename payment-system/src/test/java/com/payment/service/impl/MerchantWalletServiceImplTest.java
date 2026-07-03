package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.entity.MerchantWalletAccount;
import com.payment.entity.MerchantWalletLog;
import com.payment.mapper.MerchantWalletAccountMapper;
import com.payment.mapper.MerchantWalletLogMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MerchantWalletServiceImplTest {

    @Test
    void creditShouldRetryWhenOptimisticLockConflicts() {
        MerchantWalletAccountMapper accountMapper = mock(MerchantWalletAccountMapper.class);
        MerchantWalletLogMapper logMapper = mock(MerchantWalletLogMapper.class);
        MerchantWalletServiceImpl service = new MerchantWalletServiceImpl(accountMapper, logMapper);

        when(accountMapper.selectOne(any()))
                .thenReturn(buildMerchantAccount(1L, 2L, "50.00"))
                .thenReturn(buildMerchantAccount(1L, 2L, "50.00"));
        when(accountMapper.updateById(any(MerchantWalletAccount.class)))
                .thenReturn(0)
                .thenReturn(1);

        assertDoesNotThrow(() -> service.credit(1L, 2L, new BigDecimal("20.00"), "TEST", "BIZ-3", "retry"));
        verify(accountMapper, times(2)).updateById(any(MerchantWalletAccount.class));
        verify(logMapper, times(1)).insert(any(MerchantWalletLog.class));
    }

    @Test
    void debitShouldFailWhenBalanceBecomesInsufficientAfterConflict() {
        MerchantWalletAccountMapper accountMapper = mock(MerchantWalletAccountMapper.class);
        MerchantWalletLogMapper logMapper = mock(MerchantWalletLogMapper.class);
        MerchantWalletServiceImpl service = new MerchantWalletServiceImpl(accountMapper, logMapper);

        when(accountMapper.selectOne(any()))
                .thenReturn(buildMerchantAccount(1L, 2L, "60.00"))
                .thenReturn(buildMerchantAccount(1L, 2L, "5.00"));
        when(accountMapper.updateById(any(MerchantWalletAccount.class))).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.debit(1L, 2L, new BigDecimal("20.00"), "TEST", "BIZ-4", "debit"));
        verify(accountMapper, times(1)).updateById(any(MerchantWalletAccount.class));
    }

    private MerchantWalletAccount buildMerchantAccount(Long tenantId, Long platformUserId, String balance) {
        MerchantWalletAccount account = new MerchantWalletAccount();
        account.setId(1L);
        account.setTenantId(tenantId);
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
