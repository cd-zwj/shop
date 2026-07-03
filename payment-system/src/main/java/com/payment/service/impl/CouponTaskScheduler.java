package com.payment.service.impl;

import com.payment.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 优惠券定时任务调度器。
 * <p>
 * 基于 Spring {@code @Scheduled} 定时机制，周期性扫描并自动过期已到期但未使用的优惠券，
 * 保证优惠券状态与实际有效期一致，避免用户领取过期优惠券后无法使用造成体验问题。
 * </p>
 * <ul>
 *   <li>扫描频率通过 {@code payment.coupon.expire.fixed-delay-ms} 配置，默认 60 秒</li>
 *   <li>调用 {@link CouponService#expireCoupons} 执行批量过期，由业务层保障幂等与事务</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponTaskScheduler {

    private static final String EXPIRE_SCAN_BIZ_NO = "COUPON_EXPIRE_SCAN";
    private static final String EXPIRE_SCAN_REASON = "定时过期扫描";

    private final CouponService couponService;

    /**
     * 定时扫描并批量过期已到期的未使用优惠券。
     * <p>
     * 以固定延迟方式调度（上一次执行完成后等待指定毫秒数再执行下一次），
     * 调用 {@link CouponService#expireCoupons} 将所有已过截止时间且状态仍为未使用的优惠券标记为已过期。
     * 当实际有过期记录时打印 INFO 日志，便于运维监控。
     * </p>
     *
     * @see CouponService#expireCoupons
     */
    @Scheduled(fixedDelayString = "${payment.coupon.expire.fixed-delay-ms:60000}")
    public void expireCoupons() {
        int expired = couponService.expireCoupons(null, LocalDateTime.now(), EXPIRE_SCAN_BIZ_NO, EXPIRE_SCAN_REASON);
        if (expired > 0) {
            log.info("Expired {} coupons by scheduled scan", expired);
        }
    }
}
