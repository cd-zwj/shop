package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminPlatformUserVO {

    private Long id;
    private String userNo;
    private String username;
    private String phone;
    private String email;
    private Integer status;
    private LocalDateTime createTime;
    private Long memberTenantCount;
    private Long employeeTenantCount;
    private BigDecimal unifiedWalletBalance;
}
