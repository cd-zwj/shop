package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售趋势数据视图对象，用于返回按日统计的销售趋势图表数据。
 */
@Data
public class SalesTrendDTO {
    /** 日期（格式：yyyy-MM-dd） */
    private String date;

    /** 当日销售额 */
    private BigDecimal salesAmount;

    /** 当日订单数量 */
    private Integer orderCount;
}
