package com.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家列表VO
 */
@Data
public class MerchantListVO {
    
    /**
     * 租户ID
     */
    private Long id;
    
    /**
     * 租户编码
     */
    private String tenantCode;
    
    /**
     * 租户名称
     */
    private String name;
    
    /**
     * 联系人
     */
    private String contactName;
    
    /**
     * 联系电话
     */
    private String contactPhone;
    
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
