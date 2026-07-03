package com.payment.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 销售数据查询条件数据传输对象，用于按日期范围查询销售统计数据。
 */
@Data
public class SalesQueryDTO {
    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;
}
