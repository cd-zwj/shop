package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.annotation.RateLimit;
import com.payment.common.Result;
import com.payment.dto.AppCouponReceiveVO;
import com.payment.dto.AppCouponTemplateVO;
import com.payment.dto.AppUserCouponVO;
import com.payment.service.CouponService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C端用户优惠券控制器。
 * <p>
 * 提供可领取优惠券模板查询、我的优惠券列表查询、领取优惠券等接口。
 * 优惠券按商户隔离，用户只能操作指定商户下的优惠券。
 * 领取优惠券接口配置了限流策略，防止恶意刷券。
 * <p>
 * 路径前缀：/v1/app/tenants/{tenantId}/coupons，需要platform用户登录。
 *
 * @author payment-system
 */
@RestController
@RequestMapping("/v1/app/tenants/{tenantId}/coupons")
@RequiredArgsConstructor
public class V1AppCouponController {

    private final CouponService couponService;

    /**
     * 查询可领取的优惠券模板列表。
     * <p>
     * 获取指定商户下当前用户可以领取的所有优惠券模板，包括面额、使用条件、
     * 有效期、领取限制等信息。已领取过的模板会标记已领取状态。
     *
     * @param tenantId 商户ID，必须大于0
     * @return 可领取的优惠券模板列表
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/available")
    public Result<List<AppCouponTemplateVO>> listAvailableCoupons(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        return Result.success(couponService.listAvailableTemplates(tenantId, PlatformSessionHelper.getPlatformUserId()));
    }

    /**
     * 查询我的优惠券列表。
     * <p>
     * 获取当前用户在指定商户下已领取的优惠券列表，可按状态筛选。
     *
     * @param tenantId 商户ID，必须大于0
     * @param status   优惠券状态筛选（可选），如unused/used/expired
     * @return 用户的优惠券列表
     */
    @SaCheckLogin(type = "platform")
    @GetMapping
    public Result<List<AppUserCouponVO>> listUserCoupons(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                         @RequestParam(required = false) String status) {
        return Result.success(couponService.listUserCoupons(tenantId, PlatformSessionHelper.getPlatformUserId(), status));
    }

    /**
     * 领取优惠券。
     * <p>
     * 用户领取指定优惠券模板发放的优惠券，系统会校验库存、领取限制等。
     * 同一模板每分钟最多领取20次（防止刷接口，正常用户不太可能达到）。
     *
     * @param tenantId   商户ID，必须大于0
     * @param templateId 优惠券模板ID，必须大于0
     * @return 领取结果，包含领取的优惠券信息
     */
    @SaCheckLogin(type = "platform")
    @PostMapping("/{templateId}/receive")
    @RateLimit(prefix = "app:coupon:receive", key = "#tenantId + ':' + #templateId", window = 60, maxRequests = 20, includeIp = true)
    public Result<AppCouponReceiveVO> receiveCoupon(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable @Min(value = 1, message = "ID必须大于0") Long templateId) {
        return Result.success(couponService.receiveCouponForApp(templateId, tenantId, PlatformSessionHelper.getPlatformUserId()));
    }
}
