package com.payment.controller;

import com.payment.common.Result;
import com.payment.constant.MerchantPermission;
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
 * 商户端营销运营管理控制器（Merchant 端）。
 * <p>提供优惠券模板管理、促销活动管理、会员等级和会员标签等营销功能。
 * 所有操作均需验证当前用户是否为该租户的有效员工。</p>
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/marketing")
@RequiredArgsConstructor
@SaCheckLogin(type = "merchant")
public class V1MerchantMarketingController {

    private final V1MerchantSupportService v1MerchantSupportService;
    private final CouponService couponService;
    private final PromotionService promotionService;
    private final MemberService memberService;

    /**
     * 查询优惠券模板列表。
     * <p>
     * 获取当前商户下所有优惠券模板，可按状态筛选。
     * </p>
     *
     * @param tenantId 租户 ID，必须大于0
     * @param status   优惠券模板状态（可选），如 ACTIVE / DISABLED
     * @return 优惠券模板列表
     */
    @GetMapping("/coupons")
    public Result<List<CouponTemplateVO>> listCouponTemplates(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                            @RequestParam(required = false) String status) {
        requireMarketingPermission(tenantId);
        return Result.success(couponService.listTemplates(tenantId, status).stream()
                .map(e -> { CouponTemplateVO vo = new CouponTemplateVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    /**
     * 创建优惠券模板。
     * <p>
     * 为当前商户创建一个新的优惠券模板，模板创建后需调用激活接口方可发放。
     * </p>
     *
     * @param tenantId 租户 ID，必须大于0
     * @param dto      优惠券模板创建参数（名称、类型、门槛、折扣、数量、有效期等）
     * @return 新创建的优惠券模板信息
     */
    @PostMapping("/coupons")
    public Result<CouponTemplateVO> createCouponTemplate(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                       @Valid @RequestBody CouponTemplateCreateDTO dto) {
        requireMarketingPermission(tenantId);
        CouponTemplate entity = couponService.createTemplate(toTenantCouponTemplateDTO(tenantId, dto));
        CouponTemplateVO vo = new CouponTemplateVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    /**
     * 查询优惠券模板的使用范围列表。
     * <p>
     * 获取指定优惠券模板绑定的商品/分类等适用范围。
     * </p>
     *
     * @param tenantId   租户 ID，必须大于0
     * @param templateId 优惠券模板 ID，必须大于0
     * @return 优惠券适用范围列表
     */
    @GetMapping("/coupons/{templateId}/scopes")
    public Result<List<CouponScopeVO>> listCouponScopes(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                      @PathVariable @Min(value = 1, message = "ID必须大于0") Long templateId) {
        requireMarketingPermission(tenantId);
        return Result.success(couponService.listScopes(templateId, tenantId).stream()
                .map(e -> { CouponScopeVO vo = new CouponScopeVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    /**
     * 添加优惠券模板的使用范围。
     * <p>
     * 向指定优惠券模板追加一个商品/分类等适用范围约束。
     * </p>
     *
     * @param tenantId   租户 ID，必须大于0
     * @param templateId 优惠券模板 ID，必须大于0
     * @param dto        范围参数（范围类型、范围 ID/编码）
     * @return 新添加的优惠券范围信息
     */
    @PostMapping("/coupons/{templateId}/scopes")
    public Result<CouponScopeVO> addCouponScope(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                              @PathVariable("templateId") @Min(value = 1, message = "ID必须大于0") Long templateId,
                                              @Valid @RequestBody CouponScopeCreateDTO dto) {
        requireMarketingPermission(tenantId);
        CouponScope entity = couponService.addScope(toCouponScopeDTO(tenantId, templateId, dto));
        CouponScopeVO vo = new CouponScopeVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    /**
     * 激活优惠券模板。
     * <p>
     * 将指定优惠券模板的状态置为激活，激活后用户方可领取。会校验模板归属当前租户。
     * </p>
     *
     * @param tenantId   租户 ID，必须大于0
     * @param templateId 优惠券模板 ID，必须大于0
     * @return 操作结果
     */
    @PutMapping("/coupons/{templateId}/activate")
    public Result<Void> activateCouponTemplate(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable @Min(value = 1, message = "ID必须大于0") Long templateId) {
        requireMarketingPermission(tenantId);
        ensureCouponBelongsToTenant(tenantId, templateId);
        couponService.activateTemplate(templateId);
        return Result.success();
    }

    /**
     * 停用优惠券模板。
     * <p>
     * 将指定优惠券模板的状态置为停用，停用后用户不可再领取。会校验模板归属当前租户。
     * </p>
     *
     * @param tenantId   租户 ID，必须大于0
     * @param templateId 优惠券模板 ID，必须大于0
     * @return 操作结果
     */
    @PutMapping("/coupons/{templateId}/disable")
    public Result<Void> disableCouponTemplate(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable @Min(value = 1, message = "ID必须大于0") Long templateId) {
        requireMarketingPermission(tenantId);
        ensureCouponBelongsToTenant(tenantId, templateId);
        couponService.disableTemplate(templateId);
        return Result.success();
    }

    /**
     * 查询促销活动列表。
     * <p>
     * 获取当前商户下所有促销活动，可按状态筛选。
     * </p>
     *
     * @param tenantId 租户 ID，必须大于0
     * @param status   活动状态（可选），如 ACTIVE / DISABLED
     * @return 促销活动列表
     */
    @GetMapping("/activities")
    public Result<List<PromotionActivityVO>> listActivities(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                          @RequestParam(required = false) String status) {
        requireMarketingPermission(tenantId);
        return Result.success(promotionService.listActivities(tenantId, status).stream()
                .map(e -> { PromotionActivityVO vo = new PromotionActivityVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    /**
     * 创建促销活动。
     * <p>
     * 为当前商户创建一个新的促销活动，创建后需添加活动规则并激活方可生效。
     * </p>
     *
     * @param tenantId 租户 ID，必须大于0
     * @param dto      促销活动创建参数（名称、类型、时间范围、描述等）
     * @return 新创建的促销活动信息
     */
    @PostMapping("/activities")
    public Result<PromotionActivityVO> createActivity(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                    @Valid @RequestBody PromotionActivityCreateDTO dto) {
        requireMarketingPermission(tenantId);
        PromotionActivity entity = promotionService.createActivity(toTenantActivityDTO(tenantId, dto));
        PromotionActivityVO vo = new PromotionActivityVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    /**
     * 查询促销活动的规则列表。
     * <p>
     * 获取指定促销活动下所有规则，如满减规则、折扣规则等。
     * </p>
     *
     * @param tenantId   租户 ID，必须大于0
     * @param activityId 促销活动 ID，必须大于0
     * @return 活动规则列表
     */
    @GetMapping("/activities/{activityId}/rules")
    public Result<List<ActivityRuleVO>> listActivityRules(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                        @PathVariable @Min(value = 1, message = "ID必须大于0") Long activityId) {
        requireMarketingPermission(tenantId);
        return Result.success(promotionService.listRules(activityId, tenantId).stream()
                .map(e -> { ActivityRuleVO vo = new ActivityRuleVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    /**
     * 为促销活动添加规则。
     * <p>
     * 向指定促销活动追加一条规则，如满 100 减 20、全场 8 折等。
     * </p>
     *
     * @param tenantId   租户 ID，必须大于0
     * @param activityId 促销活动 ID，必须大于0
     * @param dto        活动规则参数（规则类型、门槛金额、折扣金额/比例、优先级等）
     * @return 新添加的活动规则信息
     */
    @PostMapping("/activities/{activityId}/rules")
    public Result<ActivityRuleVO> addActivityRule(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                @PathVariable @Min(value = 1, message = "ID必须大于0") Long activityId,
                                                @Valid @RequestBody ActivityRuleCreateDTO dto) {
        requireMarketingPermission(tenantId);
        promotionService.listRules(activityId, tenantId);
        ActivityRule entity = promotionService.addRule(toActivityRuleDTO(activityId, dto));
        ActivityRuleVO vo = new ActivityRuleVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    /**
     * 激活促销活动。
     * <p>
     * 将指定促销活动的状态置为激活，激活后活动规则将对用户生效。会校验活动归属当前租户。
     * </p>
     *
     * @param tenantId   租户 ID，必须大于0
     * @param activityId 促销活动 ID，必须大于0
     * @return 操作结果
     */
    @PutMapping("/activities/{activityId}/activate")
    public Result<Void> activateActivity(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable @Min(value = 1, message = "ID必须大于0") Long activityId) {
        requireMarketingPermission(tenantId);
        promotionService.listRules(activityId, tenantId);
        promotionService.activateActivity(activityId);
        return Result.success();
    }

    /**
     * 停用促销活动。
     * <p>
     * 将指定促销活动的状态置为停用，停用后活动规则不再对用户生效。会校验活动归属当前租户。
     * </p>
     *
     * @param tenantId   租户 ID，必须大于0
     * @param activityId 促销活动 ID，必须大于0
     * @return 操作结果
     */
    @PutMapping("/activities/{activityId}/disable")
    public Result<Void> disableActivity(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable @Min(value = 1, message = "ID必须大于0") Long activityId) {
        requireMarketingPermission(tenantId);
        promotionService.listRules(activityId, tenantId);
        promotionService.disableActivity(activityId);
        return Result.success();
    }

    /**
     * 查询会员等级列表。
     * <p>
     * 获取当前商户下所有已定义的会员等级，包括等级名称、成长值门槛、折扣率等。
     * </p>
     *
     * @param tenantId 租户 ID，必须大于0
     * @return 会员等级列表
     */
    @GetMapping("/member-levels")
    public Result<List<MemberLevelVO>> listMemberLevels(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        requireMarketingPermission(tenantId);
        return Result.success(memberService.listLevels(tenantId).stream()
                .map(e -> { MemberLevelVO vo = new MemberLevelVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    /**
     * 创建会员等级。
     * <p>
     * 为当前商户新增一个会员等级，指定等级编号、名称、成长值门槛和折扣率。
     * </p>
     *
     * @param tenantId        租户 ID，必须大于0
     * @param level           等级编号
     * @param name            等级名称
     * @param thresholdAmount 成长值门槛金额
     * @param discountRate    折扣率（可选）
     * @return 新创建的会员等级信息
     */
    @PostMapping("/member-levels")
    public Result<MemberLevelVO> createMemberLevel(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                 @RequestParam Integer level,
                                                 @RequestParam String name,
                                                 @RequestParam BigDecimal thresholdAmount,
                                                 @RequestParam(required = false) BigDecimal discountRate) {
        requireMarketingPermission(tenantId);
        MemberLevel entity = memberService.createLevel(tenantId, level, name, thresholdAmount, discountRate);
        MemberLevelVO vo = new MemberLevelVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    /**
     * 手动调整会员等级。
     * <p>
     * 商户管理员手动将指定会员的等级修改为目标等级。
     * </p>
     *
     * @param tenantId    租户 ID，必须大于0
     * @param memberId    会员 ID，必须大于0
     * @param memberLevel 目标会员等级编号
     * @return 操作结果
     */
    @PutMapping("/members/{memberId}/level")
    public Result<Void> updateMemberLevel(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                          @PathVariable @Min(value = 1, message = "ID必须大于0") Long memberId,
                                          @RequestParam Integer memberLevel) {
        requireMarketingPermission(tenantId);
        memberService.updateMemberLevel(tenantId, memberId, memberLevel);
        return Result.success();
    }

    /**
     * 查询会员标签列表。
     * <p>
     * 获取当前商户下所有已定义的会员标签，用于会员分群运营。
     * </p>
     *
     * @param tenantId 租户 ID，必须大于0
     * @return 会员标签列表
     */
    @GetMapping("/member-tags")
    public Result<List<MemberTagVO>> listMemberTags(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        requireMarketingPermission(tenantId);
        return Result.success(memberService.listTags(tenantId).stream()
                .map(e -> { MemberTagVO vo = new MemberTagVO(); BeanUtils.copyProperties(e, vo); return vo; })
                .collect(Collectors.toList()));
    }

    /**
     * 创建会员标签。
     * <p>
     * 为当前商户新增一个会员标签。
     * </p>
     *
     * @param tenantId 租户 ID，必须大于0
     * @param name     标签名称
     * @return 新创建的会员标签信息
     */
    @PostMapping("/member-tags")
    public Result<MemberTagVO> createMemberTag(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @RequestParam String name) {
        requireMarketingPermission(tenantId);
        MemberTag entity = memberService.createTag(tenantId, name);
        MemberTagVO vo = new MemberTagVO();
        BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    /**
     * 为会员分配标签。
     * <p>
     * 将指定标签绑定到指定会员，用于会员分群和精准营销。
     * </p>
     *
     * @param tenantId 租户 ID，必须大于0
     * @param memberId 会员 ID，必须大于0
     * @param tagId    标签 ID，必须大于0
     * @return 操作结果
     */
    @PutMapping("/members/{memberId}/tags/{tagId}")
    public Result<Void> assignMemberTag(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                        @PathVariable @Min(value = 1, message = "ID必须大于0") Long memberId,
                                        @PathVariable @Min(value = 1, message = "ID必须大于0") Long tagId) {
        requireMarketingPermission(tenantId);
        memberService.assignTag(tenantId, memberId, tagId);
        return Result.success();
    }

    /**
     * 移除会员标签。
     * <p>
     * 解除指定会员与指定标签的绑定关系。
     * </p>
     *
     * @param tenantId 租户 ID，必须大于0
     * @param memberId 会员 ID，必须大于0
     * @param tagId    标签 ID，必须大于0
     * @return 操作结果
     */
    @DeleteMapping("/members/{memberId}/tags/{tagId}")
    public Result<Void> removeMemberTag(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                        @PathVariable @Min(value = 1, message = "ID必须大于0") Long memberId,
                                        @PathVariable @Min(value = 1, message = "ID必须大于0") Long tagId) {
        requireMarketingPermission(tenantId);
        memberService.removeTag(tenantId, memberId, tagId);
        return Result.success();
    }

    /**
     * 校验当前用户是否具备当前租户的营销模块权限。
     *
     * @param tenantId 租户 ID
     * @throws com.payment.common.BusinessException 非有效员工时抛出
     */
    private void requireMarketingPermission(Long tenantId) {
        v1MerchantSupportService.requirePermission(tenantId, PlatformSessionHelper.getPlatformUserId(), MerchantPermission.MARKETING_MANAGE);
    }

    /**
     * 校验优惠券模板是否归属指定租户（通过查询范围时顺带校验）。
     *
     * @param tenantId   租户 ID
     * @param templateId 优惠券模板 ID
     */
    private void ensureCouponBelongsToTenant(Long tenantId, Long templateId) {
        couponService.listScopes(templateId, tenantId);
    }

    /**
     * 将前端传入的优惠券模板 DTO 转换为包含租户信息的 DTO。
     * <p>
     * 设置 tenantId 和 templateScope（TENANT），其余字段从源 DTO 复制。
     * </p>
     *
     * @param tenantId 租户 ID
     * @param source   前端传入的原始 DTO
     * @return 包含租户信息的目标 DTO
     */
    private CouponTemplateCreateDTO toTenantCouponTemplateDTO(Long tenantId, CouponTemplateCreateDTO source) {
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
        target.setTenantId(tenantId);
        target.setTemplateScope(CouponOwnerTypeEnum.TENANT.name());
        return target;
    }

    /**
     * 将前端传入的优惠券范围 DTO 转换为包含租户和模板信息的 DTO。
     *
     * @param tenantId   租户 ID
     * @param templateId 优惠券模板 ID
     * @param source     前端传入的原始 DTO
     * @return 包含租户和模板信息的目标 DTO
     */
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

    /**
     * 将前端传入的促销活动 DTO 转换为包含租户信息的 DTO。
     * <p>
     * 设置 tenantId 和 activityScope（TENANT），其余字段从源 DTO 复制。
     * </p>
     *
     * @param tenantId 租户 ID
     * @param source   前端传入的原始 DTO
     * @return 包含租户信息的目标 DTO
     */
    private PromotionActivityCreateDTO toTenantActivityDTO(Long tenantId, PromotionActivityCreateDTO source) {
        PromotionActivityCreateDTO target = new PromotionActivityCreateDTO();
        if (source != null) {
            target.setActivityName(source.getActivityName());
            target.setActivityType(source.getActivityType());
            target.setStartTime(source.getStartTime());
            target.setEndTime(source.getEndTime());
            target.setDescription(source.getDescription());
        }
        target.setTenantId(tenantId);
        target.setActivityScope(CouponOwnerTypeEnum.TENANT.name());
        return target;
    }

    /**
     * 将前端传入的活动规则 DTO 转换为包含活动 ID 的 DTO。
     *
     * @param activityId 促销活动 ID
     * @param source     前端传入的原始 DTO
     * @return 包含活动 ID 的目标 DTO
     */
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
