package com.payment.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 销售数据查询DTO
 */
@Data
public class SalesQueryDTO {
    /**
     * 开始日期
     */
    private LocalDate startDate;
    
    /**
     * 结束日期
     */
    private LocalDate endDate;
}
