package com.payment.service;

import com.payment.dto.ProductSalesRankDTO;
import com.payment.dto.SalesOverviewDTO;
import com.payment.dto.SalesQueryDTO;
import com.payment.dto.SalesTrendDTO;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 销售数据统计服务接口
 */
public interface SalesStatisticsService {
    
    /**
     * 获取销售数据概览（今日、本月销售额）
     * @param tenantId 租户ID
     * @return 销售数据概览
     */
    SalesOverviewDTO getSalesOverview(Long tenantId);
    
    /**
     * 获取销售趋势图表数据
     * @param tenantId 租户ID
     * @param query 查询条件（时间范围）
     * @return 销售趋势数据列表
     */
    List<SalesTrendDTO> getSalesTrend(Long tenantId, SalesQueryDTO query);
    
    /**
     * 获取商品销售排行
     * @param tenantId 租户ID
     * @param query 查询条件（时间范围）
     * @param limit 返回数量限制
     * @return 商品销售排行列表
     */
    List<ProductSalesRankDTO> getProductSalesRank(Long tenantId, SalesQueryDTO query, Integer limit);
    
    /**
     * 导出销售报表
     * @param tenantId 租户ID
     * @param query 查询条件（时间范围）
     * @param response HTTP响应
     */
    void exportSalesReport(Long tenantId, SalesQueryDTO query, HttpServletResponse response) throws IOException;
}
