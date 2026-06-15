package com.payment.service.impl;

import com.payment.service.MemberPointsAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 积分过期后台任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointsExpireScheduler {

    private final MemberPointsAccountService memberPointsAccountService;

    @Value("${payment.points.expire.batch-size:200}")
    private int batchSize = 200;

    /**
     * 每日扫描并扣减已到期积分。
     */
    @Scheduled(cron = "${payment.points.expire.cron:0 0 2 * * ?}")
    public void expirePoints() {
        int expiredPoints = memberPointsAccountService.expirePoints(LocalDateTime.now(), batchSize);
        if (expiredPoints > 0) {
            log.info("Expired {} member points by scheduled scan", expiredPoints);
        }
    }
}
