package com.payment.service.impl;

import com.payment.service.MemberPointsAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 积分过期后台任务调度器。
 * <p>
 * 通过 Spring {@code @Scheduled} 定时任务机制，每日凌晨 2 点（cron 可配置）
 * 自动扫描已过期的会员积分并执行批量扣减。扫描批次大小由配置项
 * {@code payment.points.expire.batch-size} 控制，默认 200 条。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointsExpireScheduler {

    private final MemberPointsAccountService memberPointsAccountService;

    @Value("${payment.points.expire.batch-size:200}")
    private int batchSize = 200;

    /**
     * 每日定时扫描并扣减已到期的会员积分。
     * <p>
     * 由 cron 表达式 {@code payment.points.expire.cron} 驱动，默认每天凌晨 2:00 执行。
     * 将当前时间作为过期截止时间，按配置的批次大小批量处理，已扣减的积分总数
     * 大于 0 时记录 INFO 级别日志。
     * </p>
     */
    @Scheduled(cron = "${payment.points.expire.cron:0 0 2 * * ?}")
    public void expirePoints() {
        int expiredPoints = memberPointsAccountService.expirePoints(LocalDateTime.now(), batchSize);
        if (expiredPoints > 0) {
            log.info("Expired {} member points by scheduled scan", expiredPoints);
        }
    }
}
