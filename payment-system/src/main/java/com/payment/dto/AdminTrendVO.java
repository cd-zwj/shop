package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端趋势时序数据。
 */
@Data
public class AdminTrendVO {

    /** 每日数据点列表。 */
    private List<TrendPoint> points;

    @Data
    public static class TrendPoint {

        /** 日期，格式 yyyy-MM-dd。 */
        private String date;

        /** 当日订单数。 */
        private Long orderCount;

        /** 当日订单金额。 */
        private BigDecimal orderAmount;

        /** 当日新增用户数。 */
        private Long newUsers;
    }
}
