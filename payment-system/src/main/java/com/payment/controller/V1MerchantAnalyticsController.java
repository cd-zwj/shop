package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.Result;
import com.payment.constant.MerchantPermission;
import com.payment.dto.ProductSalesRankDTO;
import com.payment.dto.SalesQueryDTO;
import com.payment.service.SalesStatisticsService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 商户端经营分析接口。
 * <p>面向 V1 商户工作台，提供基于 sales_order / sales_order_item 的本地经营分析数据。</p>
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/analytics")
@RequiredArgsConstructor
public class V1MerchantAnalyticsController {

    private final SalesStatisticsService salesStatisticsService;
    private final V1MerchantSupportService v1MerchantSupportService;

    /**
     * 查询商品销售排行。
     *
     * @param tenantId  租户 ID
     * @param startDate 开始日期，可选，默认最近 30 天
     * @param endDate   结束日期，可选，默认今天
     * @param limit     返回数量，默认 5，最大由服务层限制
     * @return 商品销售排行列表
     */
    @SaCheckLogin(type = "merchant")
    @GetMapping("/product-rank")
    public Result<List<ProductSalesRankDTO>> getProductSalesRank(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "5") @Min(value = 1, message = "排行数量必须大于0") Integer limit) {
        v1MerchantSupportService.requirePermission(
                tenantId,
                PlatformSessionHelper.getPlatformUserId(),
                MerchantPermission.DASHBOARD_VIEW
        );

        SalesQueryDTO query = new SalesQueryDTO();
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        return Result.success(salesStatisticsService.getV1ProductSalesRank(tenantId, query, limit));
    }
}
