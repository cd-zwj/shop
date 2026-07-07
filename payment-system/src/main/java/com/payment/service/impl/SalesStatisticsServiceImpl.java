package com.payment.service.impl;

import com.payment.dto.ProductSalesRankDTO;
import com.payment.dto.SalesOverviewDTO;
import com.payment.dto.SalesQueryDTO;
import com.payment.dto.SalesTrendDTO;
import com.payment.mapper.PaymentOrderMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.service.SalesStatisticsService;
import com.payment.util.ExcelUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 销售数据统计服务实现
 */
@Slf4j
@Service
public class SalesStatisticsServiceImpl implements SalesStatisticsService {
    
    @Autowired
    private PaymentOrderMapper paymentOrderMapper;

    @Autowired
    private SalesOrderItemMapper salesOrderItemMapper;
    
    /**
     * 获取指定租户的销售数据概览（今日/本月/累计销售额和订单数）
     *
     * @param tenantId 租户ID
     * @return 销售概览数据，包含今日、本月、累计的销售额和订单数
     */
    @Override
    public SalesOverviewDTO getSalesOverview(Long tenantId) {
        log.info("获取销售数据概览，租户ID: {}", tenantId);
        
        SalesOverviewDTO overview = new SalesOverviewDTO();
        
        // 今日数据
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        BigDecimal todaySales = paymentOrderMapper.sumSalesAmount(tenantId, todayStart, todayEnd);
        Integer todayOrderCount = paymentOrderMapper.countOrders(tenantId, todayStart, todayEnd);
        overview.setTodaySales(todaySales != null ? todaySales : BigDecimal.ZERO);
        overview.setTodayOrderCount(todayOrderCount != null ? todayOrderCount : 0);
        
        // 本月数据
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDateTime monthStart = LocalDateTime.of(firstDayOfMonth, LocalTime.MIN);
        LocalDateTime monthEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        BigDecimal monthSales = paymentOrderMapper.sumSalesAmount(tenantId, monthStart, monthEnd);
        Integer monthOrderCount = paymentOrderMapper.countOrders(tenantId, monthStart, monthEnd);
        overview.setMonthSales(monthSales != null ? monthSales : BigDecimal.ZERO);
        overview.setMonthOrderCount(monthOrderCount != null ? monthOrderCount : 0);
        
        // 累计数据
        LocalDateTime allTimeStart = LocalDateTime.of(LocalDate.of(2000, 1, 1), LocalTime.MIN);
        LocalDateTime allTimeEnd = LocalDateTime.now();
        BigDecimal totalSales = paymentOrderMapper.sumSalesAmount(tenantId, allTimeStart, allTimeEnd);
        Integer totalOrderCount = paymentOrderMapper.countOrders(tenantId, allTimeStart, allTimeEnd);
        overview.setTotalSales(totalSales != null ? totalSales : BigDecimal.ZERO);
        overview.setTotalOrderCount(totalOrderCount != null ? totalOrderCount : 0);
        
        log.info("销售数据概览查询完成: {}", overview);
        return overview;
    }
    
    /**
     * 获取指定时间范围内的销售趋势数据
     *
     * @param tenantId 租户ID
     * @param query    查询条件，包含起止日期；未指定时默认查询最近30天
     * @return 每日销售趋势列表（日期、销售额、订单数）
     */
    @Override
    public List<SalesTrendDTO> getSalesTrend(Long tenantId, SalesQueryDTO query) {
        log.info("获取销售趋势数据，租户ID: {}, 查询条件: {}", tenantId, query);
        
        // 默认查询最近30天
        LocalDate startDate = query.getStartDate() != null ? query.getStartDate() : LocalDate.now().minusDays(30);
        LocalDate endDate = query.getEndDate() != null ? query.getEndDate() : LocalDate.now();
        
        LocalDateTime startTime = LocalDateTime.of(startDate, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(endDate, LocalTime.MAX);
        
        List<SalesTrendDTO> trendList = paymentOrderMapper.selectSalesTrend(tenantId, startTime, endTime);
        log.info("销售趋势数据查询完成，共{}条记录", trendList.size());
        
        return trendList;
    }
    
    @Override
    public List<ProductSalesRankDTO> getProductSalesRank(Long tenantId, SalesQueryDTO query, Integer limit) {
        log.info("获取商品销售排行，租户ID: {}, 查询条件: {}, 限制数量: {}", tenantId, query, limit);
        
        // 默认查询最近30天
        LocalDate startDate = query.getStartDate() != null ? query.getStartDate() : LocalDate.now().minusDays(30);
        LocalDate endDate = query.getEndDate() != null ? query.getEndDate() : LocalDate.now();
        
        LocalDateTime startTime = LocalDateTime.of(startDate, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(endDate, LocalTime.MAX);
        
        // 默认返回前10名
        Integer resultLimit = limit != null && limit > 0 ? limit : 10;
        
        List<ProductSalesRankDTO> rankList = paymentOrderMapper.selectProductSalesRank(tenantId, startTime, endTime, resultLimit);
        log.info("商品销售排行查询完成，共{}条记录", rankList.size());
        
        return rankList;
    }

    @Override
    public List<ProductSalesRankDTO> getV1ProductSalesRank(Long tenantId, SalesQueryDTO query, Integer limit) {
        log.info("获取 V1 商品销售排行，租户ID: {}, 查询条件: {}, 限制数量: {}", tenantId, query, limit);

        LocalDate startDate = query.getStartDate() != null ? query.getStartDate() : LocalDate.now().minusDays(30);
        LocalDate endDate = query.getEndDate() != null ? query.getEndDate() : LocalDate.now();

        LocalDateTime startTime = LocalDateTime.of(startDate, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(endDate.plusDays(1), LocalTime.MIN);
        Integer resultLimit = limit != null && limit > 0 ? Math.min(limit, 50) : 10;

        List<ProductSalesRankDTO> rankList = salesOrderItemMapper.selectV1ProductSalesRank(
                tenantId, startTime, endTime, resultLimit);
        log.info("V1 商品销售排行查询完成，共{}条记录", rankList.size());
        return rankList;
    }
    
    @Override
    public void exportSalesReport(Long tenantId, SalesQueryDTO query, HttpServletResponse response) throws IOException {
        log.info("导出销售报表，租户ID: {}, 查询条件: {}", tenantId, query);
        
        // 默认查询最近30天
        LocalDate startDate = query.getStartDate() != null ? query.getStartDate() : LocalDate.now().minusDays(30);
        LocalDate endDate = query.getEndDate() != null ? query.getEndDate() : LocalDate.now();
        
        // 获取销售趋势数据
        List<SalesTrendDTO> trendList = getSalesTrend(tenantId, query);
        
        // 获取商品销售排行
        List<ProductSalesRankDTO> rankList = getProductSalesRank(tenantId, query, 50);
        
        // 准备Excel数据
        List<String[]> data = new ArrayList<>();
        
        // 添加报表标题信息
        data.add(new String[]{"销售报表", "", "", ""});
        data.add(new String[]{"统计时间范围", startDate + " 至 " + endDate, "", ""});
        data.add(new String[]{"", "", "", ""});
        
        // 添加销售趋势数据
        data.add(new String[]{"销售趋势", "", "", ""});
        data.add(new String[]{"日期", "销售额（元）", "订单数量", ""});
        for (SalesTrendDTO trend : trendList) {
            data.add(new String[]{
                trend.getDate(),
                trend.getSalesAmount() != null ? trend.getSalesAmount().toString() : "0",
                trend.getOrderCount() != null ? trend.getOrderCount().toString() : "0",
                ""
            });
        }
        
        // 添加空行
        data.add(new String[]{"", "", "", ""});
        
        // 添加商品销售排行数据
        data.add(new String[]{"商品销售排行", "", "", ""});
        data.add(new String[]{"商品名称", "商品编码", "销售数量", "销售额（元）"});
        for (ProductSalesRankDTO rank : rankList) {
            data.add(new String[]{
                rank.getProductName(),
                rank.getProductCode(),
                rank.getSalesQuantity() != null ? rank.getSalesQuantity().toString() : "0",
                rank.getSalesAmount() != null ? rank.getSalesAmount().toString() : "0"
            });
        }
        
        // 生成文件名
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String fileName = "销售报表_" + startDate.format(formatter) + "_" + endDate.format(formatter);
        
        // 导出Excel
        String[] headers = {"列1", "列2", "列3", "列4"};
        ExcelUtil.exportExcel(headers, data, fileName, response);
        
        log.info("销售报表导出完成");
    }
}
