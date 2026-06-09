package com.payment.controller;

import com.payment.common.Result;
import com.payment.dto.ActivityRuleCreateDTO;
import com.payment.dto.ActivityRuleVO;
import com.payment.dto.CouponScopeCreateDTO;
import com.payment.dto.CouponScopeVO;
import com.payment.dto.CouponTemplateCreateDTO;
import com.payment.dto.CouponTemplateVO;
import com.payment.dto.MemberLevelVO;
import com.payment.dto.MemberTagVO;
import com.payment.dto.PromotionActivityCreateDTO;
import com.payment.dto.PromotionActivityVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
import cn.dev33.satoken.annotation.SaCheckLogin;
import org.springframework.beans.BeanUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商户端营销运营管理接口。
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/marketing")
@RequiredArgsConstructor
@SaCheckLogin
public class V1MerchantMarketingController {

    private final V1MerchantSupportService v1MerchantSupportService;
    private final CouponService couponService;
    private final PromotionService promotionService;
    private final MemberService memberService;

    @GetMapping("/coupons")
    public Result<List<CouponTemplateVO>> listCouponTemplates(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                            @RequestParam(required = false) String status) {
        requireEmployee(tenantId);
        return Result.success(couponService.listTemplates(tenantId, status).stream()
                .map(e -> { CouponTemplateVO vo = new CouponTemplateVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    @PostMapping("/coupons")
    public Result<CouponTemplateVO> createCouponTemplate(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                       @Valid @RequestBody CouponTemplateCreateDTO dto) {
        requireEmployee(tenantId);
        CouponTemplate entity = couponService.createTemplate(toTenantCouponTemplateDTO(tenantId, dto));
        CouponTemplateVO vo = new CouponTemplateVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    @GetMapping("/coupons/{templateId}/scopes")
    public Result<List<CouponScopeVO>> listCouponScopes(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                      @PathVariable @Min(value = 1, message = "ID必须大于0") Long templateId) {
        requireEmployee(tenantId);
        return Result.success(couponService.listScopes(templateId, tenantId).stream()
                .map(e -> { CouponScopeVO vo = new CouponScopeVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    @PostMapping("/coupons/{templateId}/scopes")
    public Result<CouponScopeVO> addCouponScope(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                              @PathVariable("templateId") @Min(value = 1, message = "ID必须大于0") Long templateId,
                                              @Valid @RequestBody CouponScopeCreateDTO dto) {
        requireEmployee(tenantId);
        CouponScope entity = couponService.addScope(toCouponScopeDTO(tenantId, templateId, dto));
        CouponScopeVO vo = new CouponScopeVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    @PutMapping("/coupons/{templateId}/activate")
    public Result<Void> activateCouponTemplate(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable @Min(value = 1, message = "ID必须大于0") Long templateId) {
        requireEmployee(tenantId);
        ensureCouponBelongsToTenant(tenantId, templateId);
        couponService.activateTemplate(templateId);
        return Result.success();
    }

    @PutMapping("/coupons/{templateId}/disable")
    public Result<Void> disableCouponTemplate(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable @Min(value = 1, message = "ID必须大于0") Long templateId) {
        requireEmployee(tenantId);
        ensureCouponBelongsToTenant(tenantId, templateId);
        couponService.disableTemplate(templateId);
        return Result.success();
    }

    @GetMapping("/activities")
    public Result<List<PromotionActivityVO>> listActivities(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                          @RequestParam(required = false) String status) {
        requireEmployee(tenantId);
        return Result.success(promotionService.listActivities(tenantId, status).stream()
                .map(e -> { PromotionActivityVO vo = new PromotionActivityVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    @PostMapping("/activities")
    public Result<PromotionActivityVO> createActivity(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                    @Valid @RequestBody PromotionActivityCreateDTO dto) {
        requireEmployee(tenantId);
        PromotionActivity entity = promotionService.createActivity(toTenantActivityDTO(tenantId, dto));
        PromotionActivityVO vo = new PromotionActivityVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    @GetMapping("/activities/{activityId}/rules")
    public Result<List<ActivityRuleVO>> listActivityRules(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                        @PathVariable @Min(value = 1, message = "ID必须大于0") Long activityId) {
        requireEmployee(tenantId);
        return Result.success(promotionService.listRules(activityId, tenantId).stream()
                .map(e -> { ActivityRuleVO vo = new ActivityRuleVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    @PostMapping("/activities/{activityId}/rules")
    public Result<ActivityRuleVO> addActivityRule(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                @PathVariable @Min(value = 1, message = "ID必须大于0") Long activityId,
                                                @Valid @RequestBody ActivityRuleCreateDTO dto) {
        requireEmployee(tenantId);
        promotionService.listRules(activityId, tenantId);
        ActivityRule entity = promotionService.addRule(toActivityRuleDTO(activityId, dto));
        ActivityRuleVO vo = new ActivityRuleVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    @PutMapping("/activities/{activityId}/activate")
    public Result<Void> activateActivity(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable @Min(value = 1, message = "ID必须大于0") Long activityId) {
        requireEmployee(tenantId);
        promotionService.listRules(activityId, tenantId);
        promotionService.activateActivity(activityId);
        return Result.success();
    }

    @PutMapping("/activities/{activityId}/disable")
    public Result<Void> disableActivity(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable @Min(value = 1, message = "ID必须大于0") Long activityId) {
        requireEmployee(tenantId);
        promotionService.listRules(activityId, tenantId);
        promotionService.disableActivity(activityId);
        return Result.success();
    }

    @GetMapping("/member-levels")
    public Result<List<MemberLevelVO>> listMemberLevels(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        requireEmployee(tenantId);
        return Result.success(memberService.listLevels(tenantId).stream()
                .map(e -> { MemberLevelVO vo = new MemberLevelVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    @PostMapping("/member-levels")
    public Result<MemberLevelVO> createMemberLevel(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                 @RequestParam Integer level,
                                                 @RequestParam String name,
                                                 @RequestParam BigDecimal thresholdAmount,
                                                 @RequestParam(required = false) BigDecimal discountRate) {
        requireEmployee(tenantId);
        MemberLevel entity = memberService.createLevel(tenantId, level, name, thresholdAmount, discountRate);
        MemberLevelVO vo = new MemberLevelVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    @PutMapping("/members/{memberId}/level")
    public Result<Void> updateMemberLevel(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                          @PathVariable @Min(value = 1, message = "ID必须大于0") Long memberId,
                                          @RequestParam Integer memberLevel) {
        requireEmployee(tenantId);
        memberService.updateMemberLevel(tenantId, memberId, memberLevel);
        return Result.success();
    }

    @GetMapping("/member-tags")
    public Result<List<MemberTagVO>> listMemberTags(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        requireEmployee(tenantId);
        return Result.success(memberService.listTags(tenantId).stream()
                .map(e -> { MemberTagVO vo = new MemberTagVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    @PostMapping("/member-tags")
    public Result<MemberTagVO> createMemberTag(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @RequestParam String name) {
        requireEmployee(tenantId);
        MemberTag entity = memberService.createTag(tenantId, name);
        MemberTagVO vo = new MemberTagVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    @PutMapping("/members/{memberId}/tags/{tagId}")
    public Result<Void> assignMemberTag(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                        @PathVariable @Min(value = 1, message = "ID必须大于0") Long memberId,
                                        @PathVariable @Min(value = 1, message = "ID必须大于0") Long tagId) {
        requireEmployee(tenantId);
        memberService.assignTag(tenantId, memberId, tagId);
        return Result.success();
    }

    @DeleteMapping("/members/{memberId}/tags/{tagId}")
    public Result<Void> removeMemberTag(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                        @PathVariable @Min(value = 1, message = "ID必须大于0") Long memberId,
                                        @PathVariable @Min(value = 1, message = "ID必须大于0") Long tagId) {
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
