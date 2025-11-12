package com.payment.dto;

import lombok.Data;

/**
 * 提现查询DTO
 */
@Data
public class WithdrawalQueryDTO {
    
    /**
     * 租户ID（管理端查询时使用）
     */
    private Long tenantId;
    
    /**
     * 状态（0-待审核，1-已通过，2-已拒绝）
     */
    private Integer status;
    
    /**
     * 页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}
