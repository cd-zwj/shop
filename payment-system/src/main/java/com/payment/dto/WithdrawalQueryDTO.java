package com.payment.dto;

import lombok.Data;

/**
 * 提现记录查询条件参数。
 */
@Data
public class WithdrawalQueryDTO {

    /** 租户 ID（管理端查询时使用，按商户筛选） */
    private Long tenantId;

    /** 提现状态（0-待审核，1-已通过，2-已拒绝） */
    private Integer status;

    /** 页码（默认第 1 页） */
    private Integer pageNum = 1;

    /** 每页条数（默认 10 条） */
    private Integer pageSize = 10;
}
