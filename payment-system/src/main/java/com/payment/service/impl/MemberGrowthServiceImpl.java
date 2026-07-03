package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.entity.MemberGrowthLog;
import com.payment.entity.MemberLevel;
import com.payment.entity.TenantMember;
import com.payment.mapper.MemberGrowthLogMapper;
import com.payment.mapper.MemberLevelMapper;
import com.payment.mapper.TenantMemberMapper;
import com.payment.service.MemberGrowthService;
import com.payment.vo.MemberGrowthAccountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 会员成长值服务实现类，负责会员成长值的增减、等级升降级及有效期管理。
 * <p>
 * 核心职责：
 * <ul>
 *     <li>成长值增加/扣减，每次变动写入成长值流水日志</li>
 *     <li>成长值总额通过 member_growth_log 聚合计算（SUM），暂不引入额外汇总表</li>
 *     <li>成长值变动后自动触发等级升降级检查，支持等级有效期保护</li>
 *     <li>等级信息存储在 tenant_member.member_level 字段，member_level 表存储等级定义</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberGrowthServiceImpl implements MemberGrowthService {

    private static final int ENABLED = 1;

    private final MemberGrowthLogMapper growthLogMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final TenantMemberMapper tenantMemberMapper;

    /**
     * 增加会员成长值。
     * <p>
     * 流程：校验增长量 > 0 → 计算变动前后的成长值总额 → 写入成长值日志（EARN 类型）→ 自动检查是否满足升级条件。
     * 在同一事务中完成，保证日志写入与升级检查的原子性。
     *
     * @param platformUserId 全局平台用户ID
     * @param tenantId       租户ID
     * @param growthAmount   增加的成长值数量，必须大于0
     * @param sourceType     来源业务类型（如 ORDER、SIGN_IN 等）
     * @param sourceBizNo    来源业务单号
     * @param description    变动说明/备注
     * @throws BusinessException 当增长量不大于0时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addGrowth(Long platformUserId, Long tenantId, int growthAmount,
                          String sourceType, String sourceBizNo, String description) {
        if (growthAmount <= 0) {
            throw new BusinessException("成长值增加数量必须大于0");
        }

        int before = getTotalGrowth(platformUserId, tenantId);
        int after = before + growthAmount;

        MemberGrowthLog growthLog = new MemberGrowthLog();
        growthLog.setTenantId(tenantId);
        growthLog.setPlatformUserId(platformUserId);
        growthLog.setChangeType("EARN");
        growthLog.setChangeGrowth(growthAmount);
        growthLog.setGrowthBefore(before);
        growthLog.setGrowthAfter(after);
        growthLog.setBizType(sourceType);
        growthLog.setBizNo(sourceBizNo);
        growthLog.setRemark(description);
        growthLog.setCreateTime(LocalDateTime.now());
        growthLogMapper.insert(growthLog);

        log.info("增加成长值成功，userId={}, tenantId={}, amount={}, before={}, after={}",
                platformUserId, tenantId, growthAmount, before, after);

        // 自动检查升级
        checkAndUpgradeLevel(platformUserId, tenantId);
    }

    /**
     * 扣减会员成长值。
     * <p>
     * 流程：校验扣减量 > 0 → 校验成长值余额充足 → 计算变动前后总额 → 写入成长值日志（DEDUCT 类型）→ 自动检查是否需要降级。
     * 扣减后触发等级调整检查，确保等级与当前成长值匹配。
     *
     * @param platformUserId 全局平台用户ID
     * @param tenantId       租户ID
     * @param growthAmount   扣减的成长值数量，必须大于0且不超过当前总额
     * @param sourceType     来源业务类型（如 REFUND 等）
     * @param sourceBizNo    来源业务单号
     * @param description    变动说明/备注
     * @throws BusinessException 当扣减量不大于0或成长值余额不足时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductGrowth(Long platformUserId, Long tenantId, int growthAmount,
                             String sourceType, String sourceBizNo, String description) {
        if (growthAmount <= 0) {
            throw new BusinessException("成长值扣减数量必须大于0");
        }

        int before = getTotalGrowth(platformUserId, tenantId);
        if (before < growthAmount) {
            throw new BusinessException("成长值余额不足");
        }
        int after = before - growthAmount;

        MemberGrowthLog growthLog = new MemberGrowthLog();
        growthLog.setTenantId(tenantId);
        growthLog.setPlatformUserId(platformUserId);
        growthLog.setChangeType("DEDUCT");
        growthLog.setChangeGrowth(-growthAmount);
        growthLog.setGrowthBefore(before);
        growthLog.setGrowthAfter(after);
        growthLog.setBizType(sourceType);
        growthLog.setBizNo(sourceBizNo);
        growthLog.setRemark(description);
        growthLog.setCreateTime(LocalDateTime.now());
        growthLogMapper.insert(growthLog);

        log.info("扣减成长值成功，userId={}, tenantId={}, amount={}, before={}, after={}",
                platformUserId, tenantId, growthAmount, before, after);

        checkAndAdjustLevel(platformUserId, tenantId);
    }

    /**
     * 查询指定用户在指定租户下的成长值总额。
     * <p>
     * 通过 SQL SUM 聚合 member_growth_log 表计算，避免将全量记录拉取到内存。
     * 若无记录则返回 0。
     *
     * @param platformUserId 全局平台用户ID
     * @param tenantId       租户ID
     * @return 成长值总额（所有增加 - 所有扣减）
     */
    @Override
    public int getTotalGrowth(Long platformUserId, Long tenantId) {
        // SQL SUM 聚合，避免全量拉取记录到内存
        QueryWrapper<MemberGrowthLog> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId)
               .eq("platform_user_id", platformUserId)
               .select("IFNULL(SUM(change_growth), 0) AS change_growth");
        MemberGrowthLog result = growthLogMapper.selectOne(wrapper);
        return result != null && result.getChangeGrowth() != null ? result.getChangeGrowth() : 0;
    }

    @Override
    public MemberGrowthAccountVO getGrowthAccount(Long platformUserId, Long tenantId) {
        int totalGrowth = getTotalGrowth(platformUserId, tenantId);

        // 查询当前等级
        TenantMember member = tenantMemberMapper.selectOne(
                new LambdaQueryWrapper<TenantMember>()
                        .eq(TenantMember::getTenantId, tenantId)
                        .eq(TenantMember::getPlatformUserId, platformUserId)
        );

        Long currentLevelId = null;
        String currentLevelName = null;
        if (member != null && member.getMemberLevel() != null && member.getMemberLevel() > 0) {
            MemberLevel level = memberLevelMapper.selectOne(
                    new LambdaQueryWrapper<MemberLevel>()
                            .eq(MemberLevel::getTenantId, tenantId)
                            .eq(MemberLevel::getLevelRank, member.getMemberLevel())
                            .eq(MemberLevel::getStatus, ENABLED)
            );
            if (level != null) {
                currentLevelId = level.getId();
                currentLevelName = level.getLevelName();
            }
        }

        // 查询下一等级所需成长值
        Integer nextLevelGrowth = null;
        List<MemberLevel> levels = memberLevelMapper.selectList(
                new LambdaQueryWrapper<MemberLevel>()
                        .eq(MemberLevel::getTenantId, tenantId)
                        .eq(MemberLevel::getStatus, ENABLED)
                        .orderByAsc(MemberLevel::getLevelRank)
        );
        for (MemberLevel level : levels) {
            if (level.getUpgradeGrowth() != null && level.getUpgradeGrowth() > totalGrowth) {
                nextLevelGrowth = level.getUpgradeGrowth();
                break;
            }
        }

        return MemberGrowthAccountVO.builder()
                .totalGrowth(totalGrowth)
                .levelId(currentLevelId)
                .levelName(currentLevelName)
                .nextLevelGrowth(nextLevelGrowth)
                .build();
    }

    @Override
    public Page<MemberGrowthLog> listGrowthLogs(Long platformUserId, Long tenantId,
                                                Integer pageNum, Integer pageSize) {
        Page<MemberGrowthLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MemberGrowthLog> wrapper = new LambdaQueryWrapper<MemberGrowthLog>()
                .eq(MemberGrowthLog::getPlatformUserId, platformUserId)
                .eq(MemberGrowthLog::getTenantId, tenantId)
                .orderByDesc(MemberGrowthLog::getCreateTime);
        return growthLogMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long checkAndUpgradeLevel(Long platformUserId, Long tenantId) {
        return checkAndAdjustLevel(platformUserId, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long checkAndAdjustLevel(Long platformUserId, Long tenantId) {
        int totalGrowth = getTotalGrowth(platformUserId, tenantId);

        // 查询当前会员记录
        TenantMember member = tenantMemberMapper.selectOne(
                new LambdaQueryWrapper<TenantMember>()
                        .eq(TenantMember::getTenantId, tenantId)
                        .eq(TenantMember::getPlatformUserId, platformUserId)
        );
        if (member == null) {
            log.debug("会员记录不存在，跳过等级检查，userId={}", platformUserId);
            return null;
        }

        int currentLevelRank = member.getMemberLevel() == null ? 0 : member.getMemberLevel();

        // 查询所有启用的等级，按 levelRank 升序
        List<MemberLevel> levels = memberLevelMapper.selectList(
                new LambdaQueryWrapper<MemberLevel>()
                        .eq(MemberLevel::getTenantId, tenantId)
                        .eq(MemberLevel::getStatus, ENABLED)
                        .orderByAsc(MemberLevel::getLevelRank)
        );

        MemberLevel currentLevel = findLevelByRank(levels, currentLevelRank);
        MemberLevel targetLevel = resolveTargetLevel(levels, currentLevel, member, totalGrowth);

        if (targetLevel != null && !Objects.equals(targetLevel.getLevelRank(), currentLevelRank)) {
            member.setMemberLevel(targetLevel.getLevelRank());
            member.setUpdateTime(LocalDateTime.now());
            tenantMemberMapper.updateById(member);

            log.info("会员等级调整成功，userId={}, tenantId={}, oldLevelRank={}, newLevelRank={}, newLevelName={}, totalGrowth={}",
                    platformUserId, tenantId, currentLevelRank, targetLevel.getLevelRank(), targetLevel.getLevelName(), totalGrowth);
            return targetLevel.getId();
        }

        return null;
    }

    private MemberLevel resolveTargetLevel(List<MemberLevel> levels,
                                           MemberLevel currentLevel,
                                           TenantMember member,
                                           int totalGrowth) {
        MemberLevel qualifiedLevel = null;
        for (MemberLevel level : levels) {
            if (level.getUpgradeGrowth() != null && totalGrowth >= level.getUpgradeGrowth()) {
                qualifiedLevel = level;
            }
        }

        if (currentLevel == null) {
            return qualifiedLevel;
        }

        if (qualifiedLevel == null || currentLevel.getLevelRank() > qualifiedLevel.getLevelRank()) {
            boolean withinValidity = isWithinValidity(currentLevel, member);
            if (withinValidity && totalGrowth >= downgradeThreshold(currentLevel)) {
                return currentLevel;
            }
            return qualifiedLevel;
        }

        return qualifiedLevel;
    }

    private MemberLevel findLevelByRank(List<MemberLevel> levels, int rank) {
        for (MemberLevel level : levels) {
            if (Objects.equals(level.getLevelRank(), rank)) {
                return level;
            }
        }
        return null;
    }

    private int downgradeThreshold(MemberLevel level) {
        if (level.getDowngradeGrowth() != null) {
            return level.getDowngradeGrowth();
        }
        return level.getUpgradeGrowth() == null ? 0 : level.getUpgradeGrowth();
    }

    private boolean isWithinValidity(MemberLevel level, TenantMember member) {
        if (level.getLevelValidityDays() == null || level.getLevelValidityDays() <= 0) {
            return true;
        }
        LocalDateTime levelStartTime = member.getUpdateTime() != null ? member.getUpdateTime() : member.getCreateTime();
        if (levelStartTime == null) {
            return false;
        }
        return levelStartTime.plusDays(level.getLevelValidityDays()).isAfter(LocalDateTime.now());
    }
}
