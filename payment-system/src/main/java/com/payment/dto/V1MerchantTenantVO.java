package com.payment.dto;

import lombok.Data;

@Data
public class V1MerchantTenantVO {

    private Long tenantId;

    private String tenantName;

    private String employeeRole;
}
