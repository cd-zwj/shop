package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售数据概览DTO
 */
@Data
public class SalesOverviewDTO {
    /**
     * 今日销售额
     */
    private BigDecimal todaySales;
    
    /**
     * 今日订单数
     */
    private Integer todayOrderCount;
    
    /**
     * 本月销售额
     */
    private BigDecimal monthSales;
    
    /**
     * 本月订单数
     */
    private Integer monthOrderCount;
    
    /**
     * 累计销售额
     */
    private BigDecimal totalSales;
    
    /**
     * 累计订单数
     */
    private Integer totalOrderCount;
}
