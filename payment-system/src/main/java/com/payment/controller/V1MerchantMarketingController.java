package com.payment.controller;

import com.payment.common.Result;
import com.payment.dto.ActivityRuleCreateDTO;
import com.payment.dto.CouponScopeCreateDTO;
import com.payment.dto.CouponTemplateCreateDTO;
import com.payment.dto.PromotionActivityCreateDTO;
import jakarta.validation.Valid;
import com.payment.entity.ActivityRule;
import com.payment.entity.CouponScope;
import com.payment.entity.CouponTemplate;
import com.payment.entity.MemberLevel;
import com.payment.entity.MemberTag;
import com.payment.entity.PromotionActivity;
import com.payment.enums.CouponOwnerTypeEnum;
import com.payment.service.CouponService;
import com.payment.service.MemberService;
import com.payment.service.PromotionService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商户端营销运营管理接口。
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/marketing")
@RequiredArgsConstructor
public class V1MerchantMarketingController {

    private final V1MerchantSupportService v1MerchantSupportService;
    private final CouponService couponService;
    private final PromotionService promotionService;
    private final MemberService memberService;

    @GetMapping("/coupons")
    public Result<List<CouponTemplate>> listCouponTemplates(@PathVariable Long tenantId,
                                                            @RequestParam(required = false) String status) {
        requireEmployee(tenantId);
        return Result.success(couponService.listTemplates(tenantId, status));
    }

    @PostMapping("/coupons")
    public Result<CouponTemplate> createCouponTemplate(@PathVariable Long tenantId,
                                                       @Valid @RequestBody CouponTemplateCreateDTO dto) {
        requireEmployee(tenantId);
        return Result.success(couponService.createTemplate(toTenantCouponTemplateDTO(tenantId, dto)));
    }

    @GetMapping("/coupons/{templateId}/scopes")
    public Result<List<CouponScope>> listCouponScopes(@PathVariable Long tenantId,
                                                      @PathVariable Long templateId) {
        requireEmployee(tenantId);
        return Result.success(couponService.listScopes(templateId, tenantId));
    }

    @PostMapping("/coupons/{templateId}/scopes")
    public Result<CouponScope> addCouponScope(@PathVariable Long tenantId,
                                              @PathVariable("templateId") Long templateId,
                                              @Valid @RequestBody CouponScopeCreateDTO dto) {
        requireEmployee(tenantId);
        return Result.success(couponService.addScope(toCouponScopeDTO(tenantId, templateId, dto)));
    }

    @PutMapping("/coupons/{templateId}/activate")
    public Result<Void> activateCouponTemplate(@PathVariable Long tenantId, @PathVariable Long templateId) {
        requireEmployee(tenantId);
        ensureCouponBelongsToTenant(tenantId, templateId);
        couponService.activateTemplate(templateId);
        return Result.success();
    }

    @PutMapping("/coupons/{templateId}/disable")
    public Result<Void> disableCouponTemplate(@PathVariable Long tenantId, @PathVariable Long templateId) {
        requireEmployee(tenantId);
        ensureCouponBelongsToTenant(tenantId, templateId);
        couponService.disableTemplate(templateId);
        return Result.success();
    }

    @GetMapping("/activities")
    public Result<List<PromotionActivity>> listActivities(@PathVariable Long tenantId,
                                                          @RequestParam(required = false) String status) {
        requireEmployee(tenantId);
        return Result.success(promotionService.listActivities(tenantId, status));
    }

    @PostMapping("/activities")
    public Result<PromotionActivity> createActivity(@PathVariable Long tenantId,
                                                    @Valid @RequestBody PromotionActivityCreateDTO dto) {
        requireEmployee(tenantId);
        return Result.success(promotionService.createActivity(toTenantActivityDTO(tenantId, dto)));
    }

    @GetMapping("/activities/{activityId}/rules")
    public Result<List<ActivityRule>> listActivityRules(@PathVariable Long tenantId,
                                                        @PathVariable Long activityId) {
        requireEmployee(tenantId);
        return Result.success(promotionService.listRules(activityId, tenantId));
    }

    @PostMapping("/activities/{activityId}/rules")
    public Result<ActivityRule> addActivityRule(@PathVariable Long tenantId,
                                                @PathVariable Long activityId,
                                                @Valid @RequestBody ActivityRuleCreateDTO dto) {
        requireEmployee(tenantId);
        promotionService.listRules(activityId, tenantId);
        return Result.success(promotionService.addRule(toActivityRuleDTO(activityId, dto)));
    }

    @PutMapping("/activities/{activityId}/activate")
    public Result<Void> activateActivity(@PathVariable Long tenantId, @PathVariable Long activityId) {
        requireEmployee(tenantId);
        promotionService.listRules(activityId, tenantId);
        promotionService.activateActivity(activityId);
        return Result.success();
    }

    @PutMapping("/activities/{activityId}/disable")
    public Result<Void> disableActivity(@PathVariable Long tenantId, @PathVariable Long activityId) {
        requireEmployee(tenantId);
        promotionService.listRules(activityId, tenantId);
        promotionService.disableActivity(activityId);
        return Result.success();
    }

    @GetMapping("/member-levels")
    public Result<List<MemberLevel>> listMemberLevels(@PathVariable Long tenantId) {
        requireEmployee(tenantId);
        return Result.success(memberService.listLevels(tenantId));
    }

    @PostMapping("/member-levels")
    public Result<MemberLevel> createMemberLevel(@PathVariable Long tenantId,
                                                 @RequestParam Integer level,
                                                 @RequestParam String name,
                                                 @RequestParam BigDecimal thresholdAmount,
                                                 @RequestParam(required = false) BigDecimal discountRate) {
        requireEmployee(tenantId);
        return Result.success(memberService.createLevel(tenantId, level, name, thresholdAmount, discountRate));
    }

    @PutMapping("/members/{memberId}/level")
    public Result<Void> updateMemberLevel(@PathVariable Long tenantId,
                                          @PathVariable Long memberId,
                                          @RequestParam Integer memberLevel) {
        requireEmployee(tenantId);
        memberService.updateMemberLevel(tenantId, memberId, memberLevel);
        return Result.success();
    }

    @GetMapping("/member-tags")
    public Result<List<MemberTag>> listMemberTags(@PathVariable Long tenantId) {
        requireEmployee(tenantId);
        return Result.success(memberService.listTags(tenantId));
    }

    @PostMapping("/member-tags")
    public Result<MemberTag> createMemberTag(@PathVariable Long tenantId, @RequestParam String name) {
        requireEmployee(tenantId);
        return Result.success(memberService.createTag(tenantId, name));
    }

    @PutMapping("/members/{memberId}/tags/{tagId}")
    public Result<Void> assignMemberTag(@PathVariable Long tenantId,
                                        @PathVariable Long memberId,
                                        @PathVariable Long tagId) {
        requireEmployee(tenantId);
        memberService.assignTag(tenantId, memberId, tagId);
        return Result.success();
    }

    @DeleteMapping("/members/{memberId}/tags/{tagId}")
    public Result<Void> removeMemberTag(@PathVariable Long tenantId,
                                        @PathVariable Long memberId,
                                        @PathVariable Long tagId) {
        requireEmployee(tenantId);
        memberService.removeTag(tenantId, memberId, tagId);
        return Result.success();
    }

    private void requireEmployee(Long tenantId) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
    }

    private void ensureCouponBelongsToTenant(Long tenantId, Long templateId) {
        couponService.listScopes(templateId, tenantId);
    }

    private CouponTemplateCreateDTO toTenantCouponTemplateDTO(Long tenantId, CouponTemplateCreateDTO source) {
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
        target.setTenantId(tenantId);
        target.setOwnerType(CouponOwnerTypeEnum.TENANT.name());
        return target;
    }

    private CouponScopeCreateDTO toCouponScopeDTO(Long tenantId, Long templateId, CouponScopeCreateDTO source) {
        CouponScopeCreateDTO target = new CouponScopeCreateDTO();
        if (source != null) {
            target.setScopeType(source.getScopeType());
            target.setScopeId(source.getScopeId());
            target.setScopeCode(source.getScopeCode());
        }
        target.setCouponTemplateId(templateId);
        target.setTenantId(tenantId);
        return target;
    }

    private PromotionActivityCreateDTO toTenantActivityDTO(Long tenantId, PromotionActivityCreateDTO source) {
        PromotionActivityCreateDTO target = new PromotionActivityCreateDTO();
        if (source != null) {
            target.setName(source.getName());
            target.setActivityType(source.getActivityType());
            target.setStartTime(source.getStartTime());
            target.setEndTime(source.getEndTime());
            target.setDescription(source.getDescription());
        }
        target.setTenantId(tenantId);
        target.setOwnerType(CouponOwnerTypeEnum.TENANT.name());
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
