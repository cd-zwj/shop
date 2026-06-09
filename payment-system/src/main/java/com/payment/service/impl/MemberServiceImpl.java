package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.payment.common.BusinessException;
import com.payment.entity.MemberAccountTag;
import com.payment.entity.MemberLevel;
import com.payment.entity.MemberTag;
import com.payment.entity.SalesOrder;
import com.payment.entity.TenantMember;
import com.payment.enums.PayStatusEnum;
import com.payment.mapper.MemberAccountTagMapper;
import com.payment.mapper.MemberLevelMapper;
import com.payment.mapper.MemberTagMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.mapper.TenantMemberMapper;
import com.payment.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员运营服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private static final int ENABLED = 1;

    private final MemberLevelMapper levelMapper;
    private final MemberTagMapper tagMapper;
    private final MemberAccountTagMapper accountTagMapper;
    private final TenantMemberMapper memberMapper;
    private final SalesOrderMapper salesOrderMapper;

    @Override
    public List<MemberLevel> listLevels(Long tenantId) {
        requireId(tenantId, "商户ID不能为空");
        return levelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getTenantId, tenantId)
                .eq(MemberLevel::getStatus, ENABLED)
                .orderByAsc(MemberLevel::getLevelRank));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberLevel createLevel(Long tenantId,
                                   Integer level,
                                   String name,
                                   BigDecimal thresholdAmount,
                                   BigDecimal discountRate) {
        requireId(tenantId, "商户ID不能为空");
        if (level == null || level <= 0) {
            throw new BusinessException("会员等级必须大于0");
        }
        if (name == null || name.isBlank()) {
            throw new BusinessException("会员等级名称不能为空");
        }
        if (thresholdAmount == null || thresholdAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("累计消费门槛不能小于0");
        }
        if (discountRate != null
                && (discountRate.compareTo(BigDecimal.ZERO) <= 0 || discountRate.compareTo(BigDecimal.ONE) > 0)) {
            throw new BusinessException("会员折扣系数必须在0到1之间");
        }
        ensureLevelNotExists(tenantId, level, name.trim());

        MemberLevel entity = new MemberLevel();
        entity.setTenantId(tenantId);
        entity.setLevelRank(level);
        entity.setLevelName(name.trim());
        entity.setUpgradeGrowth(thresholdAmount.intValue());
        entity.setDiscountRate(discountRate);
        entity.setStatus(ENABLED);
        levelMapper.insert(entity);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberLevel(Long tenantId, Long memberId, Integer memberLevel) {
        TenantMember member = requireMember(tenantId, memberId);
        MemberLevel level = levelMapper.selectOne(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getTenantId, tenantId)
                .eq(MemberLevel::getLevelRank, memberLevel)
                .eq(MemberLevel::getStatus, ENABLED));
        if (level == null) {
            throw new BusinessException("会员等级不存在或未启用");
        }
        member.setMemberLevel(memberLevel);
        memberMapper.updateById(member);
    }

    @Override
    public List<MemberTag> listTags(Long tenantId) {
        requireId(tenantId, "商户ID不能为空");
        return tagMapper.selectList(new LambdaQueryWrapper<MemberTag>()
                .eq(MemberTag::getTenantId, tenantId)
                .eq(MemberTag::getStatus, ENABLED)
                .orderByDesc(MemberTag::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberTag createTag(Long tenantId, String name) {
        requireId(tenantId, "商户ID不能为空");
        if (name == null || name.isBlank()) {
            throw new BusinessException("会员标签名称不能为空");
        }
        String trimmedName = name.trim();
        MemberTag existing = tagMapper.selectOne(new LambdaQueryWrapper<MemberTag>()
                .eq(MemberTag::getTenantId, tenantId)
                .eq(MemberTag::getName, trimmedName));
        if (existing != null) {
            throw new BusinessException("会员标签已存在");
        }

        MemberTag tag = new MemberTag();
        tag.setTenantId(tenantId);
        tag.setName(trimmedName);
        tag.setStatus(ENABLED);
        tagMapper.insert(tag);
        return tag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTag(Long tenantId, Long memberId, Long tagId) {
        requireMember(tenantId, memberId);
        requireTag(tenantId, tagId);

        MemberAccountTag existing = accountTagMapper.selectOne(new LambdaQueryWrapper<MemberAccountTag>()
                .eq(MemberAccountTag::getMemberId, memberId)
                .eq(MemberAccountTag::getTagId, tagId));
        if (existing != null) {
            return;
        }

        MemberAccountTag relation = new MemberAccountTag();
        relation.setMemberId(memberId);
        relation.setTagId(tagId);
        accountTagMapper.insert(relation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTag(Long tenantId, Long memberId, Long tagId) {
        requireMember(tenantId, memberId);
        requireTag(tenantId, tagId);
        accountTagMapper.delete(new LambdaQueryWrapper<MemberAccountTag>()
                .eq(MemberAccountTag::getMemberId, memberId)
                .eq(MemberAccountTag::getTagId, tagId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndAutoUpgrade(Long tenantId, Long platformUserId) {
        if (tenantId == null || platformUserId == null) {
            return;
        }
        // 查找该用户的会员记录
        TenantMember member = memberMapper.selectOne(new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getTenantId, tenantId)
                .eq(TenantMember::getPlatformUserId, platformUserId));
        if (member == null) {
            return;
        }
        // 使用 SQL SUM 聚合代替全量查询，避免内存溢出
        Object sumResult = salesOrderMapper.selectObjs(new QueryWrapper<SalesOrder>()
                .eq("tenant_id", tenantId)
                .eq("platform_user_id", platformUserId)
                .eq("pay_status", PayStatusEnum.SUCCESS.name())
                .eq("deleted", 0)
                .select("COALESCE(SUM(total_amount), 0)")).stream().findFirst().orElse(BigDecimal.ZERO);
        BigDecimal totalSpend = sumResult instanceof BigDecimal bd ? bd : new BigDecimal(sumResult.toString());

        // 查询该租户下所有已启用的等级，按门槛降序排列
        List<MemberLevel> levels = levelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getTenantId, tenantId)
                .eq(MemberLevel::getStatus, ENABLED)
                .orderByDesc(MemberLevel::getUpgradeGrowth));
        // 从最高门槛开始匹配，找到第一个达标等级
        for (MemberLevel lv : levels) {
            if (lv.getUpgradeGrowth() != null && totalSpend.compareTo(new BigDecimal(lv.getUpgradeGrowth())) >= 0) {
                // 如果目标等级高于当前等级才升级
                if (member.getMemberLevel() == null || lv.getLevelRank() > member.getMemberLevel()) {
                    member.setMemberLevel(lv.getLevelRank());
                    memberMapper.updateById(member);
                    log.info("会员自动升级: tenantId={}, userId={}, newLevel={}, totalSpend={}",
                            tenantId, platformUserId, lv.getLevelRank(), totalSpend);
                }
                break;
            }
        }
    }

    private void ensureLevelNotExists(Long tenantId, Integer level, String name) {
        MemberLevel byLevel = levelMapper.selectOne(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getTenantId, tenantId)
                .eq(MemberLevel::getLevelRank, level));
        if (byLevel != null) {
            throw new BusinessException("会员等级数值已存在");
        }
        MemberLevel byName = levelMapper.selectOne(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getTenantId, tenantId)
                .eq(MemberLevel::getLevelName, name));
        if (byName != null) {
            throw new BusinessException("会员等级名称已存在");
        }
    }

    private TenantMember requireMember(Long tenantId, Long memberId) {
        requireId(tenantId, "商户ID不能为空");
        requireId(memberId, "会员ID不能为空");
        TenantMember member = memberMapper.selectById(memberId);
        if (member == null || !tenantId.equals(member.getTenantId())) {
            throw new BusinessException("会员不存在或不属于当前商户");
        }
        return member;
    }

    private MemberTag requireTag(Long tenantId, Long tagId) {
        requireId(tagId, "会员标签ID不能为空");
        MemberTag tag = tagMapper.selectById(tagId);
        if (tag == null || !tenantId.equals(tag.getTenantId()) || tag.getStatus() == null || tag.getStatus() != ENABLED) {
            throw new BusinessException("会员标签不存在或未启用");
        }
        return tag;
    }

    private void requireId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(message);
        }
    }
}
