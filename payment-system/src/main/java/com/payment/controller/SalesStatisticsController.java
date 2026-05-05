package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.common.Result;
import com.payment.dto.ProductSalesRankDTO;
import com.payment.dto.SalesOverviewDTO;
import com.payment.dto.SalesQueryDTO;
import com.payment.dto.SalesTrendDTO;
import com.payment.service.SalesStatisticsService;
import com.payment.util.TenantContextHolder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.List;

/**
 * 销售数据统计控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/sales")
public class SalesStatisticsController {

    @Autowired
    private SalesStatisticsService salesStatisticsService;

    /**
     * 获取销售数据概览
     */
    @SaCheckPermission("statistics:view")
    @GetMapping("/overview")
    public Result<SalesOverviewDTO> getSalesOverview() {
        log.info("商家查询销售数据概览");
        Long tenantId = TenantContextHolder.getTenantId();
        SalesOverviewDTO overview = salesStatisticsService.getSalesOverview(tenantId);
        return Result.success(overview);
    }
    
    /**
     * 获取销售趋势图表数据
     */
    @SaCheckPermission("statistics:view")
    @GetMapping("/trend")

    public Result<List<SalesTrendDTO>> getSalesTrend(
               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        log.info("商家查询销售趋势数据，开始日期: {}, 结束日期: {}", startDate, endDate);
        
        Long tenantId = TenantContextHolder.getTenantId();
        SalesQueryDTO query = new SalesQueryDTO();
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        
        List<SalesTrendDTO> trendList = salesStatisticsService.getSalesTrend(tenantId, query);
        return Result.success(trendList);
    }
    
    /**
     * 获取商品销售排行
     */
    @SaCheckPermission("statistics:view")
    @GetMapping("/product-rank")
    public Result<List<ProductSalesRankDTO>> getProductSalesRank(
               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
               @RequestParam(required = false, defaultValue = "10") Integer limit) {
        log.info("商家查询商品销售排行，开始日期: {}, 结束日期: {}, 限制数量: {}", startDate, endDate, limit);
        
        Long tenantId = TenantContextHolder.getTenantId();
        SalesQueryDTO query = new SalesQueryDTO();
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        
        List<ProductSalesRankDTO> rankList = salesStatisticsService.getProductSalesRank(tenantId, query, limit);
        return Result.success(rankList);
    }
    
    /**
     * 导出销售报表
     */
    @SaCheckPermission("statistics:export")
    @GetMapping("/export")
    public void exportSalesReport(
               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletResponse response) {
        log.info("商家导出销售报表，开始日期: {}, 结束日期: {}", startDate, endDate);
        
        try {
            Long tenantId = TenantContextHolder.getTenantId();
            SalesQueryDTO query = new SalesQueryDTO();
            query.setStartDate(startDate);
            query.setEndDate(endDate);
            
            salesStatisticsService.exportSalesReport(tenantId, query, response);
        } catch (Exception e) {
            log.error("导出销售报表失败", e);
            throw new RuntimeException("导出销售报表失败: " + e.getMessage());
        }
    }
}
