package com.payment.service.impl;

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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromotionServiceImplTest {

    @Test
    void createActivityShouldPersistDraftTenantActivity() {
        PromotionActivityMapper activityMapper = mock(PromotionActivityMapper.class);
        PromotionServiceImpl service = new PromotionServiceImpl(activityMapper, mock(ActivityRuleMapper.class));

        PromotionActivity result = service.createActivity(activityCreateDTO());

        ArgumentCaptor<PromotionActivity> captor = ArgumentCaptor.forClass(PromotionActivity.class);
        verify(activityMapper).insert(captor.capture());
        assertEquals(9L, captor.getValue().getTenantId());
        assertEquals(CouponOwnerTypeEnum.TENANT.name(), captor.getValue().getActivityScope());
        assertEquals(ActivityTypeEnum.FULL_REDUCTION.name(), captor.getValue().getActivityType());
        assertEquals("DRAFT", captor.getValue().getStatus());
        assertEquals(0, captor.getValue().getDeleted());
        assertNotNull(captor.getValue().getActivityNo());
        assertEquals(result, captor.getValue());
    }

    @Test
    void listPlatformActivitiesShouldQueryOnlyPlatformActivities() {
        PromotionActivityMapper activityMapper = mock(PromotionActivityMapper.class);
        PromotionServiceImpl service = new PromotionServiceImpl(activityMapper, mock(ActivityRuleMapper.class));
        PromotionActivity activity = activeActivity(11L, null, ActivityTypeEnum.FULL_REDUCTION.name());
        activity.setActivityScope(CouponOwnerTypeEnum.PLATFORM.name());
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));

        List<PromotionActivity> result = service.listPlatformActivities("ACTIVE");

        assertEquals(1, result.size());
        assertEquals(CouponOwnerTypeEnum.PLATFORM.name(), result.get(0).getActivityScope());
    }

    @Test
    void listPlatformRulesShouldRejectMerchantActivity() {
        PromotionActivityMapper activityMapper = mock(PromotionActivityMapper.class);
        PromotionServiceImpl service = new PromotionServiceImpl(activityMapper, mock(ActivityRuleMapper.class));
        when(activityMapper.selectById(11L)).thenReturn(activeActivity(11L, 9L, ActivityTypeEnum.FULL_REDUCTION.name()));

        assertThrows(BusinessException.class, () -> service.listPlatformRules(11L));
    }

    @Test
    void createActivityShouldRejectInvalidTimeWindow() {
        PromotionServiceImpl service = new PromotionServiceImpl(mock(PromotionActivityMapper.class), mock(ActivityRuleMapper.class));
        PromotionActivityCreateDTO dto = activityCreateDTO();
        dto.setStartTime(LocalDateTime.now().plusDays(1));
        dto.setEndTime(LocalDateTime.now());

        assertThrows(BusinessException.class, () -> service.createActivity(dto));
    }

    @Test
    void addRuleShouldPersistRuleForActivity() {
        PromotionActivityMapper activityMapper = mock(PromotionActivityMapper.class);
        ActivityRuleMapper ruleMapper = mock(ActivityRuleMapper.class);
        PromotionServiceImpl service = new PromotionServiceImpl(activityMapper, ruleMapper);
        when(activityMapper.selectById(11L)).thenReturn(activeActivity(11L, 9L, ActivityTypeEnum.FULL_REDUCTION.name()));

        ActivityRule result = service.addRule(ruleCreateDTO());

        ArgumentCaptor<ActivityRule> captor = ArgumentCaptor.forClass(ActivityRule.class);
        verify(ruleMapper).insert(captor.capture());
        assertEquals(11L, captor.getValue().getActivityId());
        assertEquals(ActivityTypeEnum.FULL_REDUCTION.name(), captor.getValue().getRuleType());
        assertEquals(new BigDecimal("100.00"), captor.getValue().getThresholdAmount());
        assertEquals(new BigDecimal("20.00"), captor.getValue().getDiscountAmount());
        assertEquals(10, captor.getValue().getPriority());
        assertEquals(0, captor.getValue().getDeleted());
        assertEquals(result, captor.getValue());
    }

    @Test
    void addRuleShouldRejectRuleOutsideActivityType() {
        PromotionActivityMapper activityMapper = mock(PromotionActivityMapper.class);
        PromotionServiceImpl service = new PromotionServiceImpl(activityMapper, mock(ActivityRuleMapper.class));
        when(activityMapper.selectById(11L)).thenReturn(activeActivity(11L, 9L, ActivityTypeEnum.FULL_REDUCTION.name()));
        ActivityRuleCreateDTO dto = ruleCreateDTO();
        dto.setRuleType(ActivityTypeEnum.DISCOUNT_RATE.name());

        assertThrows(BusinessException.class, () -> service.addRule(dto));
    }

    @Test
    void addRuleShouldRejectDuplicatePriorityInSameActivity() {
        PromotionActivityMapper activityMapper = mock(PromotionActivityMapper.class);
        ActivityRuleMapper ruleMapper = mock(ActivityRuleMapper.class);
        PromotionServiceImpl service = new PromotionServiceImpl(activityMapper, ruleMapper);
        when(activityMapper.selectById(11L)).thenReturn(activeActivity(11L, 9L, ActivityTypeEnum.FULL_REDUCTION.name()));
        when(ruleMapper.selectList(any())).thenReturn(List.of(
                rule(21L, 11L, "FULL_REDUCTION", "80.00", "10.00", null, null, null, 10)
        ));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.addRule(ruleCreateDTO()));

        assertEquals("已有相同优先级的活动规则", ex.getMessage());
    }

    @Test
    void addRuleShouldRejectDuplicateThresholdForReductionAndDiscountRules() {
        PromotionActivityMapper activityMapper = mock(PromotionActivityMapper.class);
        ActivityRuleMapper ruleMapper = mock(ActivityRuleMapper.class);
        PromotionServiceImpl service = new PromotionServiceImpl(activityMapper, ruleMapper);
        when(activityMapper.selectById(11L)).thenReturn(activeActivity(11L, 9L, ActivityTypeEnum.FULL_REDUCTION.name()));
        when(ruleMapper.selectList(any())).thenReturn(List.of(
                rule(21L, 11L, "FULL_REDUCTION", "100.00", "10.00", null, null, null, 1)
        ));
        ActivityRuleCreateDTO reduction = ruleCreateDTO();
        reduction.setPriority(2);

        BusinessException reductionEx = assertThrows(BusinessException.class, () -> service.addRule(reduction));

        assertEquals("已有相同门槛的满减规则", reductionEx.getMessage());

        PromotionActivityMapper discountActivityMapper = mock(PromotionActivityMapper.class);
        ActivityRuleMapper discountRuleMapper = mock(ActivityRuleMapper.class);
        PromotionServiceImpl discountService = new PromotionServiceImpl(discountActivityMapper, discountRuleMapper);
        when(discountActivityMapper.selectById(12L)).thenReturn(activeActivity(12L, 9L, ActivityTypeEnum.DISCOUNT_RATE.name()));
        when(discountRuleMapper.selectList(any())).thenReturn(List.of(
                rule(31L, 12L, "DISCOUNT_RATE", "200.00", null, "0.80", null, null, 1)
        ));
        ActivityRuleCreateDTO discount = ruleCreateDTO();
        discount.setActivityId(12L);
        discount.setRuleType(ActivityTypeEnum.DISCOUNT_RATE.name());
        discount.setThresholdAmount(new BigDecimal("200.00"));
        discount.setDiscountAmount(null);
        discount.setDiscountRate(new BigDecimal("0.70"));
        discount.setPriority(2);

        BusinessException discountEx = assertThrows(BusinessException.class, () -> discountService.addRule(discount));

        assertEquals("已有相同门槛的满折规则", discountEx.getMessage());
    }

    @Test
    void activateActivityShouldSetActive() {
        PromotionActivityMapper activityMapper = mock(PromotionActivityMapper.class);
        ActivityRuleMapper ruleMapper = mock(ActivityRuleMapper.class);
        PromotionServiceImpl service = new PromotionServiceImpl(activityMapper, ruleMapper);
        PromotionActivity activity = activeActivity(11L, 9L, ActivityTypeEnum.FULL_REDUCTION.name());
        activity.setStatus("DRAFT");
        when(activityMapper.selectById(11L)).thenReturn(activity);
        when(ruleMapper.selectCount(any())).thenReturn(1L);

        service.activateActivity(11L);

        ArgumentCaptor<PromotionActivity> captor = ArgumentCaptor.forClass(PromotionActivity.class);
        verify(activityMapper).updateById(captor.capture());
        assertEquals("ACTIVE", captor.getValue().getStatus());
    }

    @Test
    void disableActivityShouldSetDisabled() {
        PromotionActivityMapper activityMapper = mock(PromotionActivityMapper.class);
        PromotionServiceImpl service = new PromotionServiceImpl(activityMapper, mock(ActivityRuleMapper.class));
        when(activityMapper.selectById(11L)).thenReturn(activeActivity(11L, 9L, ActivityTypeEnum.FULL_REDUCTION.name()));

        service.disableActivity(11L);

        ArgumentCaptor<PromotionActivity> captor = ArgumentCaptor.forClass(PromotionActivity.class);
        verify(activityMapper).updateById(captor.capture());
        assertEquals("DISABLED", captor.getValue().getStatus());
    }

    @Test
    void matchPromotionsShouldCreateFullReductionCandidateForEligibleOrder() {
        PromotionActivityMapper activityMapper = mock(PromotionActivityMapper.class);
        ActivityRuleMapper ruleMapper = mock(ActivityRuleMapper.class);
        PromotionServiceImpl service = new PromotionServiceImpl(activityMapper, ruleMapper);

        PromotionActivity activity = activeActivity(11L, 9L, ActivityTypeEnum.FULL_REDUCTION.name());
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule(21L, 11L, "FULL_REDUCTION", "50.00", "8.00", null, null, null, 10)));

        List<PromotionDiscountCandidateDTO> result = service.matchPromotions(9L, List.of(
                item(1L, "drink", "30.00", 1),
                item(2L, "snack", "25.00", 1)
        ));

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getActivityId());
        assertEquals(21L, result.get(0).getActivityRuleId());
        assertEquals("FULL_REDUCTION", result.get(0).getDiscountType());
        assertEquals(new BigDecimal("8.00"), result.get(0).getDiscountAmount());
    }

    @Test
    void matchPromotionsShouldUseOnlyMatchingCategoryAmount() {
        PromotionActivityMapper activityMapper = mock(PromotionActivityMapper.class);
        ActivityRuleMapper ruleMapper = mock(ActivityRuleMapper.class);
        PromotionServiceImpl service = new PromotionServiceImpl(activityMapper, ruleMapper);

        PromotionActivity activity = activeActivity(11L, 9L, ActivityTypeEnum.FULL_REDUCTION.name());
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule(21L, 11L, "FULL_REDUCTION", "30.00", "6.00", null, null, "drink", 10)));

        List<PromotionDiscountCandidateDTO> result = service.matchPromotions(9L, List.of(
                item(1L, "drink", "20.00", 1),
                item(2L, "snack", "99.00", 1)
        ));

        assertEquals(0, result.size());
    }

    @Test
    void matchPromotionsShouldPickHighestPriorityEligibleRulePerActivity() {
        PromotionActivityMapper activityMapper = mock(PromotionActivityMapper.class);
        ActivityRuleMapper ruleMapper = mock(ActivityRuleMapper.class);
        PromotionServiceImpl service = new PromotionServiceImpl(activityMapper, ruleMapper);

        PromotionActivity activity = activeActivity(11L, 9L, ActivityTypeEnum.FULL_REDUCTION.name());
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));
        when(ruleMapper.selectList(any())).thenReturn(List.of(
                rule(21L, 11L, "FULL_REDUCTION", "50.00", "5.00", null, null, null, 1),
                rule(22L, 11L, "FULL_REDUCTION", "80.00", "12.00", null, null, null, 10)
        ));

        List<PromotionDiscountCandidateDTO> result = service.matchPromotions(9L, List.of(
                item(1L, "drink", "50.00", 2)
        ));

        assertEquals(1, result.size());
        assertEquals(22L, result.get(0).getActivityRuleId());
        assertEquals(new BigDecimal("12.00"), result.get(0).getDiscountAmount());
    }

    @Test
    void matchPromotionsShouldCalculateDiscountRateAgainstEligibleAmount() {
        PromotionActivityMapper activityMapper = mock(PromotionActivityMapper.class);
        ActivityRuleMapper ruleMapper = mock(ActivityRuleMapper.class);
        PromotionServiceImpl service = new PromotionServiceImpl(activityMapper, ruleMapper);

        PromotionActivity activity = activeActivity(12L, 9L, ActivityTypeEnum.DISCOUNT_RATE.name());
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule(31L, 12L, "DISCOUNT_RATE", "0.00", null, "0.80", 7L, null, 1)));

        List<PromotionDiscountCandidateDTO> result = service.matchPromotions(9L, List.of(
                item(7L, "drink", "40.00", 1),
                item(8L, "snack", "100.00", 1)
        ));

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("8.00"), result.get(0).getDiscountAmount());
    }

    private PromotionActivity activeActivity(Long id, Long tenantId, String type) {
        PromotionActivity activity = new PromotionActivity();
        activity.setId(id);
        activity.setTenantId(tenantId);
        activity.setActivityScope(CouponOwnerTypeEnum.TENANT.name());
        activity.setActivityType(type);
        activity.setActivityName("活动");
        activity.setStartTime(LocalDateTime.now().minusDays(1));
        activity.setEndTime(LocalDateTime.now().plusDays(1));
        activity.setStatus("ACTIVE");
        activity.setDeleted(0);
        return activity;
    }

    private PromotionActivityCreateDTO activityCreateDTO() {
        PromotionActivityCreateDTO dto = new PromotionActivityCreateDTO();
        dto.setTenantId(9L);
        dto.setActivityScope(CouponOwnerTypeEnum.TENANT.name());
        dto.setActivityName("满减活动");
        dto.setActivityType(ActivityTypeEnum.FULL_REDUCTION.name());
        dto.setStartTime(LocalDateTime.now().minusDays(1));
        dto.setEndTime(LocalDateTime.now().plusDays(7));
        return dto;
    }

    private ActivityRuleCreateDTO ruleCreateDTO() {
        ActivityRuleCreateDTO dto = new ActivityRuleCreateDTO();
        dto.setActivityId(11L);
        dto.setRuleType(ActivityTypeEnum.FULL_REDUCTION.name());
        dto.setThresholdAmount(new BigDecimal("100.00"));
        dto.setDiscountAmount(new BigDecimal("20.00"));
        dto.setPriority(10);
        return dto;
    }

    private ActivityRule rule(Long id,
                              Long activityId,
                              String type,
                              String threshold,
                              String discountAmount,
                              String discountRate,
                              Long productId,
                              String category,
                              Integer priority) {
        ActivityRule rule = new ActivityRule();
        rule.setId(id);
        rule.setActivityId(activityId);
        rule.setRuleType(type);
        rule.setThresholdAmount(new BigDecimal(threshold));
        if (discountAmount != null) {
            rule.setDiscountAmount(new BigDecimal(discountAmount));
        }
        if (discountRate != null) {
            rule.setDiscountRate(new BigDecimal(discountRate));
        }
        rule.setProductId(productId);
        rule.setCategoryCode(category);
        rule.setPriority(priority);
        rule.setDeleted(0);
        return rule;
    }

    private OrderPricingItemDTO item(Long productId, String category, String price, Integer quantity) {
        OrderPricingItemDTO item = new OrderPricingItemDTO();
        item.setProductId(productId);
        item.setCategory(category);
        item.setUnitPrice(new BigDecimal(price));
        item.setQuantity(quantity);
        return item;
    }
}
