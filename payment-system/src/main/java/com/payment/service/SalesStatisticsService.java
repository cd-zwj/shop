package com.payment.service;

import com.payment.dto.ProductSalesRankDTO;
import com.payment.dto.SalesOverviewDTO;
import com.payment.dto.SalesQueryDTO;
import com.payment.dto.SalesTrendDTO;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 销售数据统计服务接口。
 *
 * <p>提供商户维度的销售数据分析能力，包括销售概览、趋势图表、商品排行
 * 以及销售报表导出等功能。数据按租户隔离，仅统计当前商户的销售数据。</p>
 */
public interface SalesStatisticsService {

    /**
     * 获取销售数据概览（今日销售额、本月销售额、同比/环比等核心指标）。
     *
     * @param tenantId 租户ID
     * @return 销售数据概览DTO
     */
    SalesOverviewDTO getSalesOverview(Long tenantId);

    /**
     * 获取销售趋势图表数据。
     *
     * @param tenantId 租户ID
     * @param query    查询条件，包含起止日期和时间粒度
     * @return 销售趋势数据列表（按时间排列）
     */
    List<SalesTrendDTO> getSalesTrend(Long tenantId, SalesQueryDTO query);

    /**
     * 获取商品销售排行。
     *
     * @param tenantId 租户ID
     * @param query    查询条件，包含起止日期
     * @param limit    返回数量限制（如 Top 10）
     * @return 商品销售排行列表，按销售额降序
     */
    List<ProductSalesRankDTO> getProductSalesRank(Long tenantId, SalesQueryDTO query, Integer limit);

    /**
     * 获取 V1 销售订单体系下的商品销售排行。
     *
     * @param tenantId 租户ID
     * @param query    查询条件，包含起止日期
     * @param limit    返回数量限制
     * @return 商品销售排行列表
     */
    List<ProductSalesRankDTO> getV1ProductSalesRank(Long tenantId, SalesQueryDTO query, Integer limit);

    /**
     * 导出销售报表为Excel文件。
     *
     * @param tenantId 租户ID
     * @param query    查询条件，包含起止日期
     * @param response HTTP响应（用于输出Excel文件流）
     * @throws IOException 当文件写入失败时抛出
     */
    void exportSalesReport(Long tenantId, SalesQueryDTO query, HttpServletResponse response) throws IOException;
}
