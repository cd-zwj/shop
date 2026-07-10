package com.payment.service.impl;

import com.payment.service.MerchantSettlementService;
import com.payment.service.TenantConfigService;
import com.payment.service.WithdrawalService;
import com.payment.mapper.MerchantBalanceMapper;
import com.payment.mapper.MerchantWalletLogMapper;
import com.payment.entity.MerchantBalance;
import com.payment.vo.TenantConfigVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MerchantSettlementServiceImplTest {

    private static MerchantSettlementServiceImpl newService(TenantConfigService configService,
                                                             WithdrawalService withdrawalService,
                                                             MerchantBalanceMapper balanceMapper,
                                                             MerchantWalletLogMapper logMapper) {
        return new MerchantSettlementServiceImpl(configService, withdrawalService, balanceMapper, logMapper);
    }

    private static MerchantSettlementServiceImpl newService(TenantConfigService configService,
                                                             WithdrawalService withdrawalService) {
        return newService(configService, withdrawalService,
                mock(MerchantBalanceMapper.class), mock(MerchantWalletLogMapper.class));
    }

    @Test
    void settleOrderWithZeroFeeRateCreditsFullAmount() {
        TenantConfigService configService = mock(TenantConfigService.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MerchantSettlementService service = newService(configService, withdrawalService);

        when(configService.getByKey(9L, "PLATFORM_FEE_RATE"))
                .thenReturn(configWith(null));

        BigDecimal net = service.settleOrder(9L, new BigDecimal("100.00"), "SO001");

        assertEquals(new BigDecimal("100.00"), net);
        verify(withdrawalService).addMerchantBalance(eq(9L),
                eq(new BigDecimal("100.00")), eq("SO001"), eq(BigDecimal.ZERO.setScale(2)));
    }

    @Test
    void settleOrderAppliesConfiguredFeeAndCreditsNetAmount() {
        TenantConfigService configService = mock(TenantConfigService.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MerchantSettlementService service = newService(configService, withdrawalService);

        when(configService.getByKey(9L, "PLATFORM_FEE_RATE"))
                .thenReturn(configWith("3"));

        BigDecimal net = service.settleOrder(9L, new BigDecimal("100.00"), "SO002");

        // 3% of 100 = 3 -> net 97
        assertEquals(new BigDecimal("97.00"), net);
        verify(withdrawalService).addMerchantBalance(eq(9L),
                eq(new BigDecimal("97.00")), eq("SO002"), eq(new BigDecimal("3.00")));
    }

    @Test
    void settleOrderRoundsFeeToTwoDecimalPlaces() {
        TenantConfigService configService = mock(TenantConfigService.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MerchantSettlementService service = newService(configService, withdrawalService);

        when(configService.getByKey(9L, "PLATFORM_FEE_RATE"))
                .thenReturn(configWith("5"));

        // 5% of 33.33 = 1.6665 -> 1.67 (HALF_UP)
        BigDecimal net = service.settleOrder(9L, new BigDecimal("33.33"), "SO003");

        assertEquals(new BigDecimal("31.66"), net);
        verify(withdrawalService).addMerchantBalance(eq(9L),
                eq(new BigDecimal("31.66")), eq("SO003"), eq(new BigDecimal("1.67")));
    }

    @Test
    void settleOrderDoesNotCreditWhenAmountIsNotPositive() {
        TenantConfigService configService = mock(TenantConfigService.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MerchantSettlementService service = newService(configService, withdrawalService);

        // 无需 stub configService —— 金额已 <=0 时不会读取费率
        BigDecimal net = service.settleOrder(9L, BigDecimal.ZERO, "SO004");

        assertEquals(BigDecimal.ZERO, net);
        verify(withdrawalService, org.mockito.Mockito.never())
                .addMerchantBalance(any(), any(), any(), any());
    }

    @Test
    void getFeeRatePercentDefaultsToZeroWhenConfigIsMissing() {
        TenantConfigService configService = mock(TenantConfigService.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MerchantSettlementService service = newService(configService, withdrawalService);

        when(configService.getByKey(9L, "PLATFORM_FEE_RATE"))
                .thenReturn(configWith(""));

        assertEquals(0, service.getFeeRatePercent(9L).compareTo(BigDecimal.ZERO));
    }

    @Test
    void getFeeRatePercentClampsOutOfWorkspaceValuesToZero() {
        TenantConfigService configService = mock(TenantConfigService.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MerchantSettlementService service = newService(configService, withdrawalService);

        when(configService.getByKey(9L, "PLATFORM_FEE_RATE"))
                .thenReturn(configWith("150"));

        assertEquals(0, service.getFeeRatePercent(9L).compareTo(BigDecimal.ZERO));
    }

    @Test
    void getFeeRatePercentAcceptsJsonForm() {
        TenantConfigService configService = mock(TenantConfigService.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MerchantSettlementService service = newService(configService, withdrawalService);

        when(configService.getByKey(9L, "PLATFORM_FEE_RATE"))
                .thenReturn(configWith("{\"rate\":2}"));

        BigDecimal rate = service.getFeeRatePercent(9L);
        assertEquals(0, rate.compareTo(new BigDecimal("2")));
    }

    @Test
    void getFeeRatePercentReturnsZeroWhenConfigServiceThrows() {
        TenantConfigService configService = mock(TenantConfigService.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MerchantSettlementService service = newService(configService, withdrawalService);

        when(configService.getByKey(9L, "PLATFORM_FEE_RATE"))
                .thenThrow(new RuntimeException("boom"));

        assertEquals(0, service.getFeeRatePercent(9L).compareTo(BigDecimal.ZERO));
    }

    private static TenantConfigVO configWith(String value) {
        return TenantConfigVO.builder()
                .id(1L)
                .tenantId(9L)
                .configKey("PLATFORM_FEE_RATE")
                .configValue(value)
                .build();
    }
}
