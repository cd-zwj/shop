package com.payment.dto;

import lombok.Data;

/**
 * 商家查询DTO
 */
@Data
public class MerchantQueryDTO {
    
    /**
     * 商家名称（模糊查询）
     */
    private String name;
    
    /**
     * 状态：0-禁用，1-启用
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
