package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.annotation.RateLimit;
import com.payment.common.Result;
import com.payment.dto.AppCouponReceiveVO;
import com.payment.dto.AppCouponTemplateVO;
import com.payment.dto.AppUserCouponVO;
import com.payment.service.CouponService;
import com.payment.util.PlatformSessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端优惠券接口。
 */
@RestController
@RequestMapping("/v1/app/tenants/{tenantId}/coupons")
@RequiredArgsConstructor
public class V1AppCouponController {

    private final CouponService couponService;

    @SaCheckLogin
    @GetMapping("/available")
    public Result<List<AppCouponTemplateVO>> listAvailableCoupons(@PathVariable Long tenantId) {
        return Result.success(couponService.listAvailableTemplates(tenantId, PlatformSessionHelper.getPlatformUserId()));
    }

    @SaCheckLogin
    @GetMapping
    public Result<List<AppUserCouponVO>> listUserCoupons(@PathVariable Long tenantId,
                                                         @RequestParam(required = false) String status) {
        return Result.success(couponService.listUserCoupons(tenantId, PlatformSessionHelper.getPlatformUserId(), status));
    }

    @SaCheckLogin
    @PostMapping("/{templateId}/receive")
    @RateLimit(prefix = "app:coupon:receive", key = "#tenantId + ':' + #templateId", window = 60, maxRequests = 20, includeIp = true)
    public Result<AppCouponReceiveVO> receiveCoupon(@PathVariable Long tenantId, @PathVariable Long templateId) {
        return Result.success(couponService.receiveCouponForApp(templateId, tenantId, PlatformSessionHelper.getPlatformUserId()));
    }
}
