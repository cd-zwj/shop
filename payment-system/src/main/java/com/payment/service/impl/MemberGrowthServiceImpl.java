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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员成长值服务实现类。
 * <p>
 * 成长值总额通过 member_growth_log 聚合计算，暂不引入额外汇总表。
 * 等级信息存储在 tenant_member.member_level 字段（若已有），member_level 表存储等级定义。
 */
@Slf4j
@Service
public class MemberGrowthServiceImpl implements MemberGrowthService {

    @Autowired
    private MemberGrowthLogMapper growthLogMapper;

    @Autowired
    private MemberLevelMapper memberLevelMapper;

    @Autowired
    private TenantMemberMapper tenantMemberMapper;

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
    }

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
            MemberLevel level = memberLevelMapper.selectById(member.getMemberLevel());
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
                        .eq(MemberLevel::getStatus, 1)
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

        int currentLevelRank = 0;
        if (member.getMemberLevel() != null && member.getMemberLevel() > 0) {
            MemberLevel currentLevel = memberLevelMapper.selectById(member.getMemberLevel());
            if (currentLevel != null) {
                currentLevelRank = currentLevel.getLevelRank();
            }
        }

        // 查询所有启用的等级，按 levelRank 升序
        List<MemberLevel> levels = memberLevelMapper.selectList(
                new LambdaQueryWrapper<MemberLevel>()
                        .eq(MemberLevel::getTenantId, tenantId)
                        .eq(MemberLevel::getStatus, 1)
                        .orderByAsc(MemberLevel::getLevelRank)
        );

        // 找到满足条件的最高等级
        MemberLevel bestLevel = null;
        for (MemberLevel level : levels) {
            if (level.getUpgradeGrowth() != null && totalGrowth >= level.getUpgradeGrowth()
                    && level.getLevelRank() > currentLevelRank) {
                bestLevel = level;
            }
        }

        if (bestLevel != null) {
            member.setMemberLevel(bestLevel.getId().intValue());
            member.setUpdateTime(LocalDateTime.now());
            tenantMemberMapper.updateById(member);

            log.info("会员等级升级成功，userId={}, tenantId={}, newLevelId={}, newLevelName={}, totalGrowth={}",
                    platformUserId, tenantId, bestLevel.getId(), bestLevel.getLevelName(), totalGrowth);
            return bestLevel.getId();
        }

        return null;
    }
}
