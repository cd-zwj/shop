package com.payment.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 商家信息DTO
 */
@Data
public class MerchantDTO {
    
    /**
     * 租户编码（唯一）
     */
    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;
    
    /**
     * 租户名称
     */
    @NotBlank(message = "租户名称不能为空")
    private String name;
    
    /**
     * 联系人
     */
    private String contact;
    
    /**
     * 联系电话
     */
    private String phone;
    
    /**
     * 地址
     */
    private String address;
}
