package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.StoreReviewReplyDTO;
import com.payment.entity.StoreReview;
import com.payment.service.StoreReviewService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.StoreReviewVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 商户端门店评价查看和回复接口。 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/reviews")
@RequiredArgsConstructor
public class V1MerchantStoreReviewController {
    private final StoreReviewService storeReviewService;

    @SaCheckLogin(type = "merchant")
    @GetMapping
    public Result<PageResult<StoreReviewVO>> list(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                   @RequestParam(required = false) Long storeId,
                                                   @RequestParam(required = false) @Min(1) @Max(5) Integer rating,
                                                   @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
                                                   @RequestParam(defaultValue = "10") @Min(1) Integer pageSize) {
        Long operatorId = PlatformSessionHelper.getPlatformUserId();
        Page<StoreReview> page = storeReviewService.listTenantReviews(
                tenantId, operatorId, storeId, rating, pageNum, pageSize);
        return Result.success(PageResult.from(page, StoreReviewVO::from));
    }

    @SaCheckLogin(type = "merchant")
    @PutMapping("/{reviewId}/reply")
    public Result<Void> reply(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                              @PathVariable @Min(value = 1, message = "ID必须大于0") Long reviewId,
                              @Valid @RequestBody StoreReviewReplyDTO dto) {
        Long operatorId = PlatformSessionHelper.getPlatformUserId();
        storeReviewService.reply(tenantId, reviewId, operatorId, dto.getContent());
        return Result.success();
    }
}
