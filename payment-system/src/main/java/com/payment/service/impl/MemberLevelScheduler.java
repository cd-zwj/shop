package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.entity.TenantMember;
import com.payment.mapper.TenantMemberMapper;
import com.payment.service.MemberGrowthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 会员等级后台调度器。
 * <p>
 * 负责定时批量处理会员等级的自动调整任务。通过 cron 表达式配置调度周期（默认每月 1 日凌晨 3 点），
 * 分批查询状态正常且已分配等级的会员记录，逐个调用成长值服务进行等级重新计算，
 * 支持等级升降级及等级有效期复核。
 * </p>
 * <p>
 * 采用分批处理策略（每批 {@value BATCH_SIZE} 条），避免一次性加载过多数据导致内存溢出。
 * 单条记录处理失败时仅记录错误日志，不影响其他会员的等级调整，保障任务整体的容错性。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberLevelScheduler {

    private static final int ENABLED = 1;
    private static final int BATCH_SIZE = 200;

    private final TenantMemberMapper tenantMemberMapper;
    private final MemberGrowthService memberGrowthService;

    /**
     * 定期按成长值重新计算会员等级，支持降级和等级有效期复核。
     * <p>
     * 通过定时任务触发，默认每月 1 日凌晨 3 点执行（可通过 {@code payment.member.level.adjust.cron} 配置）。
     * 查询条件为：会员状态正常（{@value ENABLED}）且已分配等级，按更新时间升序排列，每批最多处理 {@value BATCH_SIZE} 条记录。
     * 逐个调用 {@link MemberGrowthService#checkAndAdjustLevel} 进行等级核算，
     * 若单条处理异常则记录错误日志后继续处理下一条，确保整体任务不会中断。
     * </p>
     */
    @Scheduled(cron = "${payment.member.level.adjust.cron:0 0 3 1 * ?}")
    public void adjustMemberLevels() {
        List<TenantMember> members = tenantMemberMapper.selectList(new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getMemberStatus, ENABLED)
                .isNotNull(TenantMember::getMemberLevel)
                .orderByAsc(TenantMember::getUpdateTime)
                .last("LIMIT " + BATCH_SIZE));

        for (TenantMember member : members) {
            try {
                memberGrowthService.checkAndAdjustLevel(member.getPlatformUserId(), member.getTenantId());
            } catch (Exception ex) {
                log.error("会员等级定时调整失败，tenantId={}, userId={}",
                        member.getTenantId(), member.getPlatformUserId(), ex);
            }
        }
    }
}
