package com.payment.service.impl;

import com.payment.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 优惠券后台任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponTaskScheduler {

    private static final String EXPIRE_SCAN_BIZ_NO = "COUPON_EXPIRE_SCAN";
    private static final String EXPIRE_SCAN_REASON = "定时过期扫描";

    private final CouponService couponService;

    /**
     * 扫描并过期已到期的未使用优惠券。
     */
    @Scheduled(fixedDelayString = "${payment.coupon.expire.fixed-delay-ms:60000}")
    public void expireCoupons() {
        int expired = couponService.expireCoupons(null, LocalDateTime.now(), EXPIRE_SCAN_BIZ_NO, EXPIRE_SCAN_REASON);
        if (expired > 0) {
            log.info("Expired {} coupons by scheduled scan", expired);
        }
    }
}
