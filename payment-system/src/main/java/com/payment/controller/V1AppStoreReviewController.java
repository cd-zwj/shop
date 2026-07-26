package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.StoreReviewCreateDTO;
import com.payment.entity.StoreReview;
import com.payment.service.StoreReviewService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.StoreReviewVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** C端门店评价接口。 */
@RestController
@RequestMapping("/v1/app/tenants/{tenantId}")
@RequiredArgsConstructor
public class V1AppStoreReviewController {
    private final StoreReviewService storeReviewService;

    @SaCheckLogin(type = "platform")
    @PostMapping("/orders/{orderNo}/review")
    public Result<StoreReviewVO> create(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                        @PathVariable String orderNo,
                                        @Valid @RequestBody StoreReviewCreateDTO dto) {
        return Result.success(StoreReviewVO.from(storeReviewService.create(
                PlatformSessionHelper.getPlatformUserId(), tenantId, orderNo, dto)));
    }

    @SaCheckLogin(type = "platform")
    @GetMapping("/orders/{orderNo}/review")
    public Result<StoreReviewVO> getMine(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                         @PathVariable String orderNo) {
        return Result.success(StoreReviewVO.from(storeReviewService.getMine(
                PlatformSessionHelper.getPlatformUserId(), tenantId, orderNo)));
    }

    @GetMapping("/stores/{storeId}/reviews")
    public Result<PageResult<StoreReviewVO>> listVisible(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                          @PathVariable @Min(value = 1, message = "ID必须大于0") Long storeId,
                                                          @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer pageNum,
                                                          @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer pageSize) {
        Page<StoreReview> page = storeReviewService.listVisibleReviews(tenantId, storeId, pageNum, pageSize);
        return Result.success(PageResult.from(page, StoreReviewVO::from));
    }
}
