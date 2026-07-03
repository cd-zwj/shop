package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.Result;
import com.payment.dto.CouponEffectVO;
import com.payment.dto.MarketingEffectSummaryVO;
import com.payment.service.MarketingEffectService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/marketing")
@RequiredArgsConstructor
@SaCheckLogin(type = "merchant")
public class V1MerchantMarketingEffectController {

    private final MarketingEffectService marketingEffectService;

    @GetMapping("/effect/summary")
    public Result<MarketingEffectSummaryVO> summary(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        return Result.success(marketingEffectService.getSummary(
                tenantId, PlatformSessionHelper.getPlatformUserId()));
    }

    @GetMapping("/coupons/{templateId}/effect")
    public Result<CouponEffectVO> couponEffect(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long templateId) {
        return Result.success(marketingEffectService.getCouponEffect(
                tenantId, PlatformSessionHelper.getPlatformUserId(), templateId));
    }
}
