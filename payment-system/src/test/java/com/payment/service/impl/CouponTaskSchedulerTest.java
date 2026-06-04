package com.payment.service.impl;

import com.payment.service.CouponService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CouponTaskSchedulerTest {

    @Test
    void expireCouponTaskShouldDelegateToCouponServiceWithScanBizNo() {
        CouponService couponService = mock(CouponService.class);
        CouponTaskScheduler scheduler = new CouponTaskScheduler(couponService);

        scheduler.expireCoupons();

        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(couponService).expireCoupons(org.mockito.ArgumentMatchers.isNull(), timeCaptor.capture(), org.mockito.ArgumentMatchers.eq("COUPON_EXPIRE_SCAN"),
                org.mockito.ArgumentMatchers.eq("定时过期扫描"));
        assertTrue(!timeCaptor.getValue().isAfter(LocalDateTime.now()));
    }
}
