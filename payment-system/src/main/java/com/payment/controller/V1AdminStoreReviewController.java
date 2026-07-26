package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.common.Result;
import com.payment.dto.StoreReviewModerationDTO;
import com.payment.service.StoreReviewService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 平台处理违规门店评价。 */
@RestController
@RequestMapping("/v1/admin/reviews")
@RequiredArgsConstructor
public class V1AdminStoreReviewController {
    private final StoreReviewService storeReviewService;

    @SaCheckPermission(type = "admin", value = "admin:trade:detail")
    @PutMapping("/{reviewId}/moderation")
    public Result<Void> moderate(@PathVariable @Min(value = 1, message = "ID必须大于0") Long reviewId,
                                 @Valid @RequestBody StoreReviewModerationDTO dto) {
        storeReviewService.moderate(reviewId, PlatformSessionHelper.getPlatformUserId(), dto.getVisible(), dto.getRemark());
        return Result.success();
    }
}
