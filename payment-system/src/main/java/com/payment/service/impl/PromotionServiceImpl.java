package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.dto.ActivityRuleCreateDTO;
import com.payment.dto.PromotionActivityCreateDTO;
import com.payment.dto.pricing.OrderPricingItemDTO;
import com.payment.dto.pricing.PromotionDiscountCandidateDTO;
import com.payment.entity.ActivityRule;
import com.payment.entity.PromotionActivity;
import com.payment.enums.ActivityTypeEnum;
import com.payment.enums.CouponOwnerTypeEnum;
import com.payment.mapper.ActivityRuleMapper;
import com.payment.mapper.PromotionActivityMapper;
import com.payment.service.PromotionService;
import com.payment.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 营销活动服务实现类。
 */
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final PromotionActivityMapper promotionActivityMapper;
    private final ActivityRuleMapper activityRuleMapper;

    @Override
    public List<PromotionActivity> listActivities(Long tenantId, String status) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("商户ID不能为空");
        }
        List<PromotionActivity> activities = promotionActivityMapper.selectList(new LambdaQueryWrapper<PromotionActivity>()
                .eq(PromotionActivity::getOwnerType, CouponOwnerTypeEnum.TENANT.name())
                .eq(PromotionActivity::getTenantId, tenantId)
                .eq(PromotionActivity::getDeleted, 0)
                .eq(status != null && !status.isBlank(), PromotionActivity::getStatus, status)
                .orderByDesc(PromotionActivity::getCreateTime));
        return activities == null ? Collections.emptyList() : activities;
    }

    @Override
    public List<PromotionActivity> listPlatformActivities(String status) {
        List<PromotionActivity> activities = promotionActivityMapper.selectList(new LambdaQueryWrapper<PromotionActivity>()
                .eq(PromotionActivity::getOwnerType, CouponOwnerTypeEnum.PLATFORM.name())
                .eq(PromotionActivity::getDeleted, 0)
                .eq(status != null && !status.isBlank(), PromotionActivity::getStatus, status)
                .orderByDesc(PromotionActivity::getCreateTime));
        return activities == null ? Collections.emptyList() : activities;
    }

    @Override
    public List<ActivityRule> listRules(Long activityId, Long tenantId) {
        PromotionActivity activity = requireActivity(activityId);
        if (!Objects.equals(activity.getTenantId(), tenantId)) {
            throw new BusinessException("营销活动不属于当前商户");
        }
        List<ActivityRule> rules = activityRuleMapper.selectList(new LambdaQueryWrapper<ActivityRule>()
                .eq(ActivityRule::getActivityId, activityId)
                .eq(ActivityRule::getDeleted, 0)
                .orderByDesc(ActivityRule::getPriority));
        return rules == null ? Collections.emptyList() : rules;
    }

    @Override
    public List<ActivityRule> listPlatformRules(Long activityId) {
        PromotionActivity activity = requireActivity(activityId);
        if (!CouponOwnerTypeEnum.PLATFORM.name().equals(activity.getOwnerType())) {
            throw new BusinessException("营销活动不是平台活动");
        }
        List<ActivityRule> rules = activityRuleMapper.selectList(new LambdaQueryWrapper<ActivityRule>()
                .eq(ActivityRule::getActivityId, activityId)
                .eq(ActivityRule::getDeleted, 0)
                .orderByDesc(ActivityRule::getPriority));
        return rules == null ? Collections.emptyList() : rules;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromotionActivity createActivity(PromotionActivityCreateDTO dto) {
        validateActivityCreate(dto);
        LocalDateTime now = LocalDateTime.now();
        PromotionActivity activity = new PromotionActivity();
        activity.setActivityNo(BizNoGenerator.generate("PA"));
        activity.setTenantId(CouponOwnerTypeEnum.PLATFORM.name().equals(dto.getOwnerType()) ? null : dto.getTenantId());
        activity.setOwnerType(dto.getOwnerType());
        activity.setName(dto.getName().trim());
        activity.setActivityType(dto.getActivityType());
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        activity.setStatus(STATUS_DRAFT);
        activity.setDescription(trimToNull(dto.getDescription()));
        activity.setDeleted(0);
        activity.setCreateTime(now);
        activity.setUpdateTime(now);
        promotionActivityMapper.insert(activity);
        return activity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityRule addRule(ActivityRuleCreateDTO dto) {
        if (dto == null) {
            throw new BusinessException("营销活动规则不能为空");
        }
        PromotionActivity activity = requireActivity(dto.getActivityId());
        ActivityTypeEnum ruleType = parseEnum(ActivityTypeEnum.class, dto.getRuleType(), "营销活动规则类型不合法");
        if (!Objects.equals(activity.getActivityType(), ruleType.name())) {
            throw new BusinessException("活动规则类型必须与活动类型一致");
        }
        validateRule(ruleType, dto);

        ActivityRule rule = new ActivityRule();
        rule.setActivityId(activity.getId());
        rule.setRuleType(ruleType.name());
        rule.setThresholdAmount(safe(dto.getThresholdAmount()));
        rule.setDiscountAmount(dto.getDiscountAmount());
        rule.setDiscountRate(dto.getDiscountRate());
        rule.setProductId(dto.getProductId());
        rule.setCategoryCode(trimToNull(dto.getCategoryCode()));
        rule.setRuleConfigJson(trimToNull(dto.getRuleConfigJson()));
        rule.setPriority(dto.getPriority() == null ? 0 : dto.getPriority());
        rule.setDeleted(0);
        activityRuleMapper.insert(rule);
        return rule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateActivity(Long activityId) {
        PromotionActivity activity = requireActivity(activityId);
        validateActivityWindow(activity.getStartTime(), activity.getEndTime());
        Long ruleCount = activityRuleMapper.selectCount(
                new LambdaQueryWrapper<ActivityRule>()
                        .eq(ActivityRule::getActivityId, activityId)
                        .eq(ActivityRule::getDeleted, 0));
        if (ruleCount == null || ruleCount == 0) {
            throw new BusinessException("活动无规则，不能激活");
        }
        activity.setStatus(STATUS_ACTIVE);
        activity.setUpdateTime(LocalDateTime.now());
        promotionActivityMapper.updateById(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableActivity(Long activityId) {
        PromotionActivity activity = requireActivity(activityId);
        activity.setStatus(STATUS_DISABLED);
        activity.setUpdateTime(LocalDateTime.now());
        promotionActivityMapper.updateById(activity);
    }

    @Override
    public List<PromotionDiscountCandidateDTO> matchPromotions(Long tenantId, List<OrderPricingItemDTO> items) {
        List<PromotionActivity> activities = listActiveActivities(tenantId);
        if (activities.isEmpty()) {
            return Collections.emptyList();
        }

        return activities.stream()
                .map(activity -> matchActivity(activity, items))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void validateActivityCreate(PromotionActivityCreateDTO dto) {
        if (dto == null) {
            throw new BusinessException("营销活动不能为空");
        }
        CouponOwnerTypeEnum ownerType = parseEnum(CouponOwnerTypeEnum.class, dto.getOwnerType(), "营销活动归属类型不合法");
        parseEnum(ActivityTypeEnum.class, dto.getActivityType(), "营销活动类型不合法");
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("营销活动名称不能为空");
        }
        if (ownerType == CouponOwnerTypeEnum.TENANT && (dto.getTenantId() == null || dto.getTenantId() <= 0)) {
            throw new BusinessException("商户活动必须绑定商户");
        }
        validateActivityWindow(dto.getStartTime(), dto.getEndTime());
    }

    private void validateActivityWindow(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException("营销活动时间不能为空");
        }
        if (startTime.isAfter(endTime)) {
            throw new BusinessException("营销活动开始时间不能晚于结束时间");
        }
    }

    private void validateRule(ActivityTypeEnum ruleType, ActivityRuleCreateDTO dto) {
        if (ruleType == ActivityTypeEnum.FULL_REDUCTION) {
            if (safe(dto.getThresholdAmount()).compareTo(BigDecimal.ZERO) <= 0
                    || safe(dto.getDiscountAmount()).compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("满减活动必须配置正数门槛和优惠金额");
            }
            return;
        }
        if (ruleType == ActivityTypeEnum.DISCOUNT_RATE) {
            BigDecimal rate = dto.getDiscountRate();
            if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
                throw new BusinessException("折扣活动折扣比例必须大于0且小于1");
            }
            return;
        }
        throw new BusinessException("当前活动规则暂不支持自动计算");
    }

    private PromotionActivity requireActivity(Long activityId) {
        if (activityId == null || activityId <= 0) {
            throw new BusinessException("营销活动ID不能为空");
        }
        PromotionActivity activity = promotionActivityMapper.selectById(activityId);
        if (activity == null || Integer.valueOf(1).equals(activity.getDeleted())) {
            throw new BusinessException("营销活动不存在");
        }
        return activity;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        try {
            return Enum.valueOf(enumType, value.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(message);
        }
    }

    private List<PromotionActivity> listActiveActivities(Long tenantId) {
        LocalDateTime now = LocalDateTime.now();
        List<PromotionActivity> activities = promotionActivityMapper.selectList(new LambdaQueryWrapper<PromotionActivity>()
                .eq(PromotionActivity::getStatus, "ACTIVE")
                .eq(PromotionActivity::getDeleted, 0)
                .le(PromotionActivity::getStartTime, now)
                .ge(PromotionActivity::getEndTime, now)
                .and(wrapper -> wrapper
                        .eq(PromotionActivity::getOwnerType, CouponOwnerTypeEnum.PLATFORM.name())
                        .or(inner -> inner
                                .eq(PromotionActivity::getOwnerType, CouponOwnerTypeEnum.TENANT.name())
                                .eq(PromotionActivity::getTenantId, tenantId)))
                .orderByDesc(PromotionActivity::getCreateTime));
        return activities == null ? Collections.emptyList() : activities;
    }

    private PromotionDiscountCandidateDTO matchActivity(PromotionActivity activity, List<OrderPricingItemDTO> items) {
        List<ActivityRule> rules = activityRuleMapper.selectList(new LambdaQueryWrapper<ActivityRule>()
                .eq(ActivityRule::getActivityId, activity.getId())
                .eq(ActivityRule::getDeleted, 0)
                .orderByDesc(ActivityRule::getPriority));
        if (rules == null || rules.isEmpty()) {
            return null;
        }

        return rules.stream()
                .map(rule -> toCandidate(activity, rule, items))
                .filter(Objects::nonNull)
                .max(Comparator.comparing(candidate -> resolvePriority(rules, candidate.getActivityRuleId())))
                .orElse(null);
    }

    private PromotionDiscountCandidateDTO toCandidate(PromotionActivity activity,
                                                      ActivityRule rule,
                                                      List<OrderPricingItemDTO> items) {
        BigDecimal eligibleAmount = calculateEligibleAmount(rule, items);
        if (eligibleAmount.compareTo(safe(rule.getThresholdAmount())) < 0) {
            return null;
        }

        BigDecimal discountAmount = calculateDiscountAmount(rule, eligibleAmount);
        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        PromotionDiscountCandidateDTO candidate = new PromotionDiscountCandidateDTO();
        candidate.setActivityId(activity.getId());
        candidate.setActivityRuleId(rule.getId());
        candidate.setDiscountType(rule.getRuleType());
        candidate.setDiscountAmount(discountAmount);
        candidate.setRuleSnapshotJson(buildRuleSnapshot(activity, rule, eligibleAmount));
        return candidate;
    }

    private BigDecimal calculateDiscountAmount(ActivityRule rule, BigDecimal eligibleAmount) {
        if (ActivityTypeEnum.DISCOUNT_RATE.name().equals(rule.getRuleType())) {
            BigDecimal rate = rule.getDiscountRate();
            if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            return scale(eligibleAmount.multiply(BigDecimal.ONE.subtract(rate)));
        }
        if (ActivityTypeEnum.FULL_REDUCTION.name().equals(rule.getRuleType())) {
            return scale(safe(rule.getDiscountAmount()).min(eligibleAmount));
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateEligibleAmount(ActivityRule rule, List<OrderPricingItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .filter(item -> matchesRuleScope(rule, item))
                .map(item -> safe(item.getUnitPrice()).multiply(BigDecimal.valueOf(defaultQuantity(item))))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean matchesRuleScope(ActivityRule rule, OrderPricingItemDTO item) {
        boolean hasProductScope = rule.getProductId() != null;
        boolean hasCategoryScope = rule.getCategoryCode() != null && !rule.getCategoryCode().isBlank();
        if (!hasProductScope && !hasCategoryScope) {
            return true;
        }
        return Objects.equals(rule.getProductId(), item.getProductId())
                || Objects.equals(rule.getCategoryCode(), item.getCategory());
    }

    private int resolvePriority(List<ActivityRule> rules, Long ruleId) {
        return rules.stream()
                .filter(rule -> Objects.equals(rule.getId(), ruleId))
                .map(ActivityRule::getPriority)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(0);
    }

    private String buildRuleSnapshot(PromotionActivity activity, ActivityRule rule, BigDecimal eligibleAmount) {
        return "{\"activityNo\":\"" + activity.getActivityNo()
                + "\",\"activityType\":\"" + activity.getActivityType()
                + "\",\"ruleType\":\"" + rule.getRuleType()
                + "\",\"eligibleAmount\":\"" + eligibleAmount
                + "\"}";
    }

    private int defaultQuantity(OrderPricingItemDTO item) {
        return item.getQuantity() == null || item.getQuantity() <= 0 ? 0 : item.getQuantity();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return safe(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
