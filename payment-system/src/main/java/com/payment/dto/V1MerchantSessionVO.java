package com.payment.dto;

import lombok.Data;

import java.util.List;

@Data
public class V1MerchantSessionVO {

    private String token;

    private long expiresIn;

    private Long platformUserId;

    private String username;

    private Long tenantId;

    private String tenantName;

    private String employeeRole;

    private List<V1MerchantTenantVO> tenants;
}
