package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.common.Result;
import com.payment.dto.ActivityRuleCreateDTO;
import com.payment.dto.ActivityRuleVO;
import com.payment.dto.CouponScopeCreateDTO;
import com.payment.dto.CouponScopeVO;
import com.payment.dto.CouponTemplateCreateDTO;
import com.payment.dto.CouponTemplateVO;
import com.payment.dto.PromotionActivityCreateDTO;
import com.payment.dto.PromotionActivityVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import com.payment.entity.ActivityRule;
import com.payment.entity.CouponScope;
import com.payment.entity.CouponTemplate;
import com.payment.entity.PromotionActivity;
import com.payment.enums.CouponOwnerTypeEnum;
import com.payment.service.CouponService;
import com.payment.service.PromotionService;
import org.springframework.beans.BeanUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
    public Result<List<CouponTemplateVO>> listPlatformCouponTemplates(@RequestParam(required = false) String status) {
        return Result.success(couponService.listPlatformTemplates(status).stream()
                .map(e -> { CouponTemplateVO vo = new CouponTemplateVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    @SaCheckPermission("admin:marketing:create")
    @PostMapping("/coupons")
    public Result<CouponTemplateVO> createPlatformCouponTemplate(@Valid @RequestBody CouponTemplateCreateDTO dto) {
        CouponTemplate entity = couponService.createTemplate(toPlatformCouponTemplateDTO(dto));
        CouponTemplateVO vo = new CouponTemplateVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    @SaCheckPermission("admin:marketing:list")
    @GetMapping("/coupons/{templateId}/scopes")
    public Result<List<CouponScopeVO>> listCouponScopes(@PathVariable @Min(value = 1, message = "ID必须大于0") Long templateId) {
        return Result.success(couponService.listPlatformScopes(templateId).stream()
                .map(e -> { CouponScopeVO vo = new CouponScopeVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    @SaCheckPermission("admin:marketing:create")
    @PostMapping("/coupons/{templateId}/scopes")
    public Result<CouponScopeVO> addCouponScope(@PathVariable @Min(value = 1, message = "ID必须大于0") Long templateId,
                                              @Valid @RequestBody CouponScopeCreateDTO dto) {
        couponService.listPlatformScopes(templateId);
        CouponScope entity = couponService.addScope(toCouponScopeDTO(templateId, dto));
        CouponScopeVO vo = new CouponScopeVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    @SaCheckPermission("admin:marketing:update")
    @PutMapping("/coupons/{templateId}/activate")
    public Result<Void> activateCouponTemplate(@PathVariable @Min(value = 1, message = "ID必须大于0") Long templateId) {
        couponService.listPlatformScopes(templateId);
        couponService.activateTemplate(templateId);
        return Result.success();
    }

    @SaCheckPermission("admin:marketing:update")
    @PutMapping("/coupons/{templateId}/disable")
    public Result<Void> disableCouponTemplate(@PathVariable @Min(value = 1, message = "ID必须大于0") Long templateId) {
        couponService.listPlatformScopes(templateId);
        couponService.disableTemplate(templateId);
        return Result.success();
    }

    @SaCheckPermission("admin:marketing:list")
    @GetMapping("/activities")
    public Result<List<PromotionActivityVO>> listPlatformActivities(@RequestParam(required = false) String status) {
        return Result.success(promotionService.listPlatformActivities(status).stream()
                .map(e -> { PromotionActivityVO vo = new PromotionActivityVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    @SaCheckPermission("admin:marketing:create")
    @PostMapping("/activities")
    public Result<PromotionActivityVO> createPlatformActivity(@Valid @RequestBody PromotionActivityCreateDTO dto) {
        PromotionActivity entity = promotionService.createActivity(toPlatformActivityDTO(dto));
        PromotionActivityVO vo = new PromotionActivityVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    @SaCheckPermission("admin:marketing:list")
    @GetMapping("/activities/{activityId}/rules")
    public Result<List<ActivityRuleVO>> listActivityRules(@PathVariable @Min(value = 1, message = "ID必须大于0") Long activityId) {
        return Result.success(promotionService.listPlatformRules(activityId).stream()
                .map(e -> { ActivityRuleVO vo = new ActivityRuleVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    @SaCheckPermission("admin:marketing:create")
    @PostMapping("/activities/{activityId}/rules")
    public Result<ActivityRuleVO> addActivityRule(@PathVariable @Min(value = 1, message = "ID必须大于0") Long activityId,
                                                @Valid @RequestBody ActivityRuleCreateDTO dto) {
        promotionService.listPlatformRules(activityId);
        ActivityRule entity = promotionService.addRule(toActivityRuleDTO(activityId, dto));
        ActivityRuleVO vo = new ActivityRuleVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    @SaCheckPermission("admin:marketing:update")
    @PutMapping("/activities/{activityId}/activate")
    public Result<Void> activateActivity(@PathVariable @Min(value = 1, message = "ID必须大于0") Long activityId) {
        promotionService.listPlatformRules(activityId);
        promotionService.activateActivity(activityId);
        return Result.success();
    }

    @SaCheckPermission("admin:marketing:update")
    @PutMapping("/activities/{activityId}/disable")
    public Result<Void> disableActivity(@PathVariable @Min(value = 1, message = "ID必须大于0") Long activityId) {
        promotionService.listPlatformRules(activityId);
        promotionService.disableActivity(activityId);
        return Result.success();
    }

    private CouponTemplateCreateDTO toPlatformCouponTemplateDTO(CouponTemplateCreateDTO source) {
        CouponTemplateCreateDTO target = new CouponTemplateCreateDTO();
        if (source != null) {
            target.setTemplateName(source.getTemplateName());
            target.setCouponType(source.getCouponType());
            target.setThresholdAmount(source.getThresholdAmount());
            target.setDiscountAmount(source.getDiscountAmount());
            target.setDiscountRate(source.getDiscountRate());
            target.setMaxDiscountAmount(source.getMaxDiscountAmount());
            target.setTotalQuantity(source.getTotalQuantity());
            target.setPerUserLimit(source.getPerUserLimit());
            target.setReceiveStartTime(source.getReceiveStartTime());
            target.setReceiveEndTime(source.getReceiveEndTime());
            target.setValidDays(source.getValidDays());
            target.setValidStartTime(source.getValidStartTime());
            target.setValidEndTime(source.getValidEndTime());
            target.setDescription(source.getDescription());
        }
        target.setTenantId(null);
        target.setTemplateScope(CouponOwnerTypeEnum.PLATFORM.name());
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
            target.setActivityName(source.getActivityName());
            target.setActivityType(source.getActivityType());
            target.setStartTime(source.getStartTime());
            target.setEndTime(source.getEndTime());
            target.setDescription(source.getDescription());
        }
        target.setTenantId(null);
        target.setActivityScope(CouponOwnerTypeEnum.PLATFORM.name());
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
