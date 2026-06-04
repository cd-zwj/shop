package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.common.Result;
import com.payment.dto.ActivityRuleCreateDTO;
import com.payment.dto.CouponScopeCreateDTO;
import com.payment.dto.CouponTemplateCreateDTO;
import com.payment.dto.PromotionActivityCreateDTO;
import jakarta.validation.Valid;
import com.payment.entity.ActivityRule;
import com.payment.entity.CouponScope;
import com.payment.entity.CouponTemplate;
import com.payment.entity.PromotionActivity;
import com.payment.enums.CouponOwnerTypeEnum;
import com.payment.service.CouponService;
import com.payment.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端营销运营管理接口。
 */
@RestController
@RequestMapping("/v1/admin/marketing")
@RequiredArgsConstructor
public class V1AdminMarketingController {

    private final CouponService couponService;
    private final PromotionService promotionService;

    @SaCheckPermission("admin:marketing:list")
    @GetMapping("/coupons")
    public Result<List<CouponTemplate>> listPlatformCouponTemplates(@RequestParam(required = false) String status) {
        return Result.success(couponService.listPlatformTemplates(status));
    }

    @SaCheckPermission("admin:marketing:create")
    @PostMapping("/coupons")
    public Result<CouponTemplate> createPlatformCouponTemplate(@Valid @RequestBody CouponTemplateCreateDTO dto) {
        return Result.success(couponService.createTemplate(toPlatformCouponTemplateDTO(dto)));
    }

    @SaCheckPermission("admin:marketing:list")
    @GetMapping("/coupons/{templateId}/scopes")
    public Result<List<CouponScope>> listCouponScopes(@PathVariable Long templateId) {
        return Result.success(couponService.listPlatformScopes(templateId));
    }

    @SaCheckPermission("admin:marketing:create")
    @PostMapping("/coupons/{templateId}/scopes")
    public Result<CouponScope> addCouponScope(@PathVariable Long templateId,
                                              @Valid @RequestBody CouponScopeCreateDTO dto) {
        couponService.listPlatformScopes(templateId);
        return Result.success(couponService.addScope(toCouponScopeDTO(templateId, dto)));
    }

    @SaCheckPermission("admin:marketing:update")
    @PutMapping("/coupons/{templateId}/activate")
    public Result<Void> activateCouponTemplate(@PathVariable Long templateId) {
        couponService.listPlatformScopes(templateId);
        couponService.activateTemplate(templateId);
        return Result.success();
    }

    @SaCheckPermission("admin:marketing:update")
    @PutMapping("/coupons/{templateId}/disable")
    public Result<Void> disableCouponTemplate(@PathVariable Long templateId) {
        couponService.listPlatformScopes(templateId);
        couponService.disableTemplate(templateId);
        return Result.success();
    }

    @SaCheckPermission("admin:marketing:list")
    @GetMapping("/activities")
    public Result<List<PromotionActivity>> listPlatformActivities(@RequestParam(required = false) String status) {
        return Result.success(promotionService.listPlatformActivities(status));
    }

    @SaCheckPermission("admin:marketing:create")
    @PostMapping("/activities")
    public Result<PromotionActivity> createPlatformActivity(@Valid @RequestBody PromotionActivityCreateDTO dto) {
        return Result.success(promotionService.createActivity(toPlatformActivityDTO(dto)));
    }

    @SaCheckPermission("admin:marketing:list")
    @GetMapping("/activities/{activityId}/rules")
    public Result<List<ActivityRule>> listActivityRules(@PathVariable Long activityId) {
        return Result.success(promotionService.listPlatformRules(activityId));
    }

    @SaCheckPermission("admin:marketing:create")
    @PostMapping("/activities/{activityId}/rules")
    public Result<ActivityRule> addActivityRule(@PathVariable Long activityId,
                                                @Valid @RequestBody ActivityRuleCreateDTO dto) {
        promotionService.listPlatformRules(activityId);
        return Result.success(promotionService.addRule(toActivityRuleDTO(activityId, dto)));
    }

    @SaCheckPermission("admin:marketing:update")
    @PutMapping("/activities/{activityId}/activate")
    public Result<Void> activateActivity(@PathVariable Long activityId) {
        promotionService.listPlatformRules(activityId);
        promotionService.activateActivity(activityId);
        return Result.success();
    }

    @SaCheckPermission("admin:marketing:update")
    @PutMapping("/activities/{activityId}/disable")
    public Result<Void> disableActivity(@PathVariable Long activityId) {
        promotionService.listPlatformRules(activityId);
        promotionService.disableActivity(activityId);
        return Result.success();
    }

    private CouponTemplateCreateDTO toPlatformCouponTemplateDTO(CouponTemplateCreateDTO source) {
        CouponTemplateCreateDTO target = new CouponTemplateCreateDTO();
        if (source != null) {
            target.setName(source.getName());
            target.setCouponType(source.getCouponType());
            target.setThresholdAmount(source.getThresholdAmount());
            target.setDiscountAmount(source.getDiscountAmount());
            target.setDiscountRate(source.getDiscountRate());
            target.setMaxDiscountAmount(source.getMaxDiscountAmount());
            target.setTotalStock(source.getTotalStock());
            target.setPerUserLimit(source.getPerUserLimit());
            target.setReceiveStartTime(source.getReceiveStartTime());
            target.setReceiveEndTime(source.getReceiveEndTime());
            target.setValidDaysAfterReceive(source.getValidDaysAfterReceive());
            target.setValidStartTime(source.getValidStartTime());
            target.setValidEndTime(source.getValidEndTime());
            target.setMinMemberLevel(source.getMinMemberLevel());
            target.setExcludeMemberTagIds(source.getExcludeMemberTagIds());
            target.setStackStrategy(source.getStackStrategy());
            target.setDescription(source.getDescription());
        }
        target.setTenantId(null);
        target.setOwnerType(CouponOwnerTypeEnum.PLATFORM.name());
        return target;
    }

    private CouponScopeCreateDTO toCouponScopeDTO(Long templateId, CouponScopeCreateDTO source) {
        CouponScopeCreateDTO target = new CouponScopeCreateDTO();
        if (source != null) {
            target.setScopeType(source.getScopeType());
            target.setScopeId(source.getScopeId());
            target.setScopeCode(source.getScopeCode());
            target.setTenantId(source.getTenantId());
        }
        target.setCouponTemplateId(templateId);
        return target;
    }

    private PromotionActivityCreateDTO toPlatformActivityDTO(PromotionActivityCreateDTO source) {
        PromotionActivityCreateDTO target = new PromotionActivityCreateDTO();
        if (source != null) {
            target.setName(source.getName());
            target.setActivityType(source.getActivityType());
            target.setStartTime(source.getStartTime());
            target.setEndTime(source.getEndTime());
            target.setDescription(source.getDescription());
        }
        target.setTenantId(null);
        target.setOwnerType(CouponOwnerTypeEnum.PLATFORM.name());
        return target;
    }

    private ActivityRuleCreateDTO toActivityRuleDTO(Long activityId, ActivityRuleCreateDTO source) {
        ActivityRuleCreateDTO target = new ActivityRuleCreateDTO();
        if (source != null) {
            target.setRuleType(source.getRuleType());
            target.setThresholdAmount(source.getThresholdAmount());
            target.setDiscountAmount(source.getDiscountAmount());
            target.setDiscountRate(source.getDiscountRate());
            target.setProductId(source.getProductId());
            target.setCategoryCode(source.getCategoryCode());
            target.setRuleConfigJson(source.getRuleConfigJson());
            target.setPriority(source.getPriority());
        }
        target.setActivityId(activityId);
        return target;
    }
}
