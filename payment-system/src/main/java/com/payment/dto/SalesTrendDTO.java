package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售趋势数据DTO
 */
@Data
public class SalesTrendDTO {
    /**
     * 日期（格式：yyyy-MM-dd）
     */
    private String date;
    
    /**
     * 销售额
     */
    private BigDecimal salesAmount;
    
    /**
     * 订单数量
     */
    private Integer orderCount;
}
