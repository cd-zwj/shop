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
 * 会员等级后台任务。
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
