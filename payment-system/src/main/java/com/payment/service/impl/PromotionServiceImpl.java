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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 营销活动服务实现类。
 * <p>
 * 负责营销活动（PromotionActivity）及其规则（ActivityRule）的全生命周期管理，
 * 包括活动的创建、查询、激活、停用，以及规则的添加与校验。
 * 同时提供订单级别的促销匹配能力，根据活动规则计算满减、折扣等优惠候选，
 * 供计价引擎选择最优优惠方案。
 * </p>
 * <p>
 * 业务约束：活动规则类型必须与活动类型一致；满减规则门槛和优惠金额必须为正数；
 * 折扣比例必须在 (0, 1) 区间内。激活活动前必须至少存在一条有效规则。
 * </p>
 *
 * @see PromotionService
 * @see PromotionActivity
 * @see ActivityRule
 */
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final PromotionActivityMapper promotionActivityMapper;
    private final ActivityRuleMapper activityRuleMapper;

    /**
     * 查询指定商户的营销活动列表。
     *
     * @param tenantId 商户ID，不能为空且必须大于0
     * @param status   活动状态过滤条件，为 null 时不过滤
     * @return 商户活动列表，按创建时间倒序排列；无数据时返回空列表
     */
    @Override
    public List<PromotionActivity> listActivities(Long tenantId, String status) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("商户ID不能为空");
        }
        List<PromotionActivity> activities = promotionActivityMapper.selectList(new LambdaQueryWrapper<PromotionActivity>()
                .eq(PromotionActivity::getActivityScope, CouponOwnerTypeEnum.TENANT.name())
                .eq(PromotionActivity::getTenantId, tenantId)
                .eq(PromotionActivity::getDeleted, 0)
                .eq(status != null && !status.isBlank(), PromotionActivity::getStatus, status)
                .orderByDesc(PromotionActivity::getCreateTime));
        return activities == null ? Collections.emptyList() : activities;
    }

    /**
     * 查询平台级别的营销活动列表。
     *
     * @param status 活动状态过滤条件，为 null 时不过滤
     * @return 平台活动列表，按创建时间倒序排列；无数据时返回空列表
     */
    @Override
    public List<PromotionActivity> listPlatformActivities(String status) {
        List<PromotionActivity> activities = promotionActivityMapper.selectList(new LambdaQueryWrapper<PromotionActivity>()
                .eq(PromotionActivity::getActivityScope, CouponOwnerTypeEnum.PLATFORM.name())
                .eq(PromotionActivity::getDeleted, 0)
                .eq(status != null && !status.isBlank(), PromotionActivity::getStatus, status)
                .orderByDesc(PromotionActivity::getCreateTime));
        return activities == null ? Collections.emptyList() : activities;
    }

    /**
     * 查询指定商户活动下的规则列表。
     *
     * @param activityId 活动ID
     * @param tenantId   商户ID，用于校验活动归属
     * @return 规则列表，按优先级倒序排列；无数据时返回空列表
     * @throws BusinessException 活动不存在或不属于当前商户时抛出
     */
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

    /**
     * 查询平台级别活动的规则列表。
     *
     * @param activityId 活动 ID
     * @return 规则列表，按优先级倒序排列；无数据时返回空列表
     * @throws BusinessException 活动不存在或不是平台活动时抛出
     */
    @Override
    public List<ActivityRule> listPlatformRules(Long activityId) {
        PromotionActivity activity = requireActivity(activityId);
        if (!CouponOwnerTypeEnum.PLATFORM.name().equals(activity.getActivityScope())) {
            throw new BusinessException("营销活动不是平台活动");
        }
        List<ActivityRule> rules = activityRuleMapper.selectList(new LambdaQueryWrapper<ActivityRule>()
                .eq(ActivityRule::getActivityId, activityId)
                .eq(ActivityRule::getDeleted, 0)
                .orderByDesc(ActivityRule::getPriority));
        return rules == null ? Collections.emptyList() : rules;
    }

    /**
     * 创建营销活动。
     * <p>
     * 校验活动参数合法性后创建草稿状态的活动记录。平台活动不需要绑定商户。
     *
     * @param dto 活动创建 DTO，包含活动名称、类型、时间窗口、归属类型等
     * @return 新创建的活动实体（状态为 DRAFT）
     * @throws BusinessException 参数校验失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromotionActivity createActivity(PromotionActivityCreateDTO dto) {
        validateActivityCreate(dto);
        LocalDateTime now = LocalDateTime.now();
        PromotionActivity activity = new PromotionActivity();
        activity.setActivityNo(BizNoGenerator.generate("PA"));
        activity.setTenantId(CouponOwnerTypeEnum.PLATFORM.name().equals(dto.getActivityScope()) ? null : dto.getTenantId());
        activity.setActivityScope(dto.getActivityScope());
        activity.setActivityName(dto.getActivityName().trim());
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

    /**
     * 为营销活动添加规则。
     * <p>
     * 校验活动存在性和归属后，校验规则类型与活动类型一致性，
     * 然后校验规则参数合法性（满减需正数门槛和金额，折扣比例在 0~1 之间）。
     *
     * @param dto 规则创建 DTO，包含活动 ID、规则类型、门槛金额、折扣金额/比例等
     * @return 新创建的活动规则实体
     * @throws BusinessException 规则类型不匹配或参数不合法时抛出
     */
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
        validateRuleConflicts(ruleType, dto);

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

    /**
     * 激活营销活动。
     * <p>
     * 校验活动时间窗口和至少存在一条有效规则后，将状态从 DRAFT 变更为 ACTIVE。
     *
     * @param activityId 活动 ID
     * @throws BusinessException 活动时间无效或无规则时抛出
     */
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

    /**
     * 停用营销活动，将状态变更为 DISABLED。
     *
     * @param activityId 活动 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableActivity(Long activityId) {
        PromotionActivity activity = requireActivity(activityId);
        activity.setStatus(STATUS_DISABLED);
        activity.setUpdateTime(LocalDateTime.now());
        promotionActivityMapper.updateById(activity);
    }

    /**
     * 匹配订单适用的促销活动优惠候选列表。
     * <p>
     * 查询当前生效的活动（平台级 + 当前商户级），批量加载规则消除 N+1 查询，
     * 逐活动按规则优先级匹配订单商品，返回满足门槛的优惠候选列表供计价引擎选择最优方案。
     *
     * @param tenantId 租户 ID
     * @param items    订单商品列表
     * @return 可用的促销优惠候选列表，无匹配时返回空列表
     */
    @Override
    public List<PromotionDiscountCandidateDTO> matchPromotions(Long tenantId, List<OrderPricingItemDTO> items) {
        List<PromotionActivity> activities = listActiveActivities(tenantId);
        if (activities.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量预加载所有活动的规则，消除 N+1 查询
        Set<Long> activityIds = activities.stream()
                .map(PromotionActivity::getId)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, List<ActivityRule>> rulesMap = batchLoadRules(activityIds);

        return activities.stream()
                .map(activity -> matchActivityWithRules(activity, rulesMap.getOrDefault(activity.getId(), Collections.emptyList()), items))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /** 校验营销活动创建参数合法性。 */
    private void validateActivityCreate(PromotionActivityCreateDTO dto) {
        if (dto == null) {
            throw new BusinessException("营销活动不能为空");
        }
        CouponOwnerTypeEnum activityScope = parseEnum(CouponOwnerTypeEnum.class, dto.getActivityScope(), "营销活动归属类型不合法");
        parseEnum(ActivityTypeEnum.class, dto.getActivityType(), "营销活动类型不合法");
        if (dto.getActivityName() == null || dto.getActivityName().isBlank()) {
            throw new BusinessException("营销活动名称不能为空");
        }
        if (activityScope == CouponOwnerTypeEnum.TENANT && (dto.getTenantId() == null || dto.getTenantId() <= 0)) {
            throw new BusinessException("商户活动必须绑定商户");
        }
        validateActivityWindow(dto.getStartTime(), dto.getEndTime());
    }

    /** 校验活动时间窗口合法性。 */
    private void validateActivityWindow(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException("营销活动时间不能为空");
        }
        if (startTime.isAfter(endTime)) {
            throw new BusinessException("营销活动开始时间不能晚于结束时间");
        }
    }

    /** 校验活动规则参数（满减门槛/金额或折扣比例）。 */
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

    /** 校验同一活动内的规则冲突，避免结算命中顺序和优惠含义不清晰。 */
    private void validateRuleConflicts(ActivityTypeEnum ruleType, ActivityRuleCreateDTO dto) {
        List<ActivityRule> existingRules = activityRuleMapper.selectList(new LambdaQueryWrapper<ActivityRule>()
                .eq(ActivityRule::getActivityId, dto.getActivityId())
                .eq(ActivityRule::getDeleted, 0));
        if (existingRules == null || existingRules.isEmpty()) {
            return;
        }

        int priority = dto.getPriority() == null ? 0 : dto.getPriority();
        boolean duplicatePriority = existingRules.stream()
                .map(ActivityRule::getPriority)
                .map(value -> value == null ? 0 : value)
                .anyMatch(value -> value == priority);
        if (duplicatePriority) {
            throw new BusinessException("已有相同优先级的活动规则");
        }

        BigDecimal threshold = safe(dto.getThresholdAmount());
        boolean duplicateThreshold = existingRules.stream()
                .filter(rule -> ruleType.name().equals(rule.getRuleType()))
                .map(ActivityRule::getThresholdAmount)
                .map(this::safe)
                .anyMatch(value -> value.compareTo(threshold) == 0);
        if (duplicateThreshold && ruleType == ActivityTypeEnum.FULL_REDUCTION) {
            throw new BusinessException("已有相同门槛的满减规则");
        }
        if (duplicateThreshold && ruleType == ActivityTypeEnum.DISCOUNT_RATE) {
            throw new BusinessException("已有相同门槛的满折规则");
        }
    }

    /**
     * 根据活动 ID 查询活动实体。
     *
     * @param activityId 活动 ID
     * @return 活动实体
     * @throws BusinessException 活动不存在或 ID 非法时抛出
     */
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

    /**
     * 查询当前生效的活动列表（平台级 + 当前商户级）。
     *
     * @param tenantId 租户 ID
     * @return 在有效时间窗口内的 ACTIVE 状态活动列表
     */
    private List<PromotionActivity> listActiveActivities(Long tenantId) {
        LocalDateTime now = LocalDateTime.now();
        List<PromotionActivity> activities = promotionActivityMapper.selectList(new LambdaQueryWrapper<PromotionActivity>()
                .eq(PromotionActivity::getStatus, "ACTIVE")
                .eq(PromotionActivity::getDeleted, 0)
                .le(PromotionActivity::getStartTime, now)
                .ge(PromotionActivity::getEndTime, now)
                .and(wrapper -> wrapper
                        .eq(PromotionActivity::getActivityScope, CouponOwnerTypeEnum.PLATFORM.name())
                        .or(inner -> inner
                                .eq(PromotionActivity::getActivityScope, CouponOwnerTypeEnum.TENANT.name())
                                .eq(PromotionActivity::getTenantId, tenantId)))
                .orderByDesc(PromotionActivity::getCreateTime));
        return activities == null ? Collections.emptyList() : activities;
    }

    /**
     * 匹配单个活动的最优规则（按优先级排序，取第一个满足门槛的）。
     *
     * @param activity 活动实体
     * @param items    订单商品列表
     * @return 最优优惠候选，无匹配时返回 null
     */
    private PromotionDiscountCandidateDTO matchActivity(PromotionActivity activity, List<OrderPricingItemDTO> items) {
        List<ActivityRule> rules = activityRuleMapper.selectList(new LambdaQueryWrapper<ActivityRule>()
                .eq(ActivityRule::getActivityId, activity.getId())
                .eq(ActivityRule::getDeleted, 0)
                .orderByDesc(ActivityRule::getPriority));
        return matchActivityWithRules(activity, rules == null ? Collections.emptyList() : rules, items);
    }

    /**
     * 使用预加载的规则列表匹配活动的最优优惠候选。
     *
     * @param activity 活动实体
     * @param rules    活动规则列表（按优先级降序）
     * @param items    订单商品列表
     * @return 最优优惠候选，无匹配时返回 null
     */
    private PromotionDiscountCandidateDTO matchActivityWithRules(PromotionActivity activity,
                                                                  List<ActivityRule> rules,
                                                                  List<OrderPricingItemDTO> items) {
        if (rules.isEmpty()) {
            return null;
        }

        return rules.stream()
                .map(rule -> toCandidate(activity, rule, items))
                .filter(Objects::nonNull)
                .max(Comparator.comparing(candidate -> resolvePriority(rules, candidate.getActivityRuleId())))
                .orElse(null);
    }

    /** 批量加载多个活动的规则，消除 N+1 查询 */
    private Map<Long, List<ActivityRule>> batchLoadRules(Set<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ActivityRule> allRules = activityRuleMapper.selectList(new LambdaQueryWrapper<ActivityRule>()
                .in(ActivityRule::getActivityId, activityIds)
                .eq(ActivityRule::getDeleted, 0)
                .orderByDesc(ActivityRule::getPriority));
        if (allRules == null || allRules.isEmpty()) {
            return Collections.emptyMap();
        }
        return allRules.stream()
                .collect(Collectors.groupingBy(ActivityRule::getActivityId));
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
