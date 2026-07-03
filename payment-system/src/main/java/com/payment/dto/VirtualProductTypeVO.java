package com.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VirtualProductTypeVO {
    private Long id;
    private Long tenantId;
    private String typeCode;
    private String typeName;
    private String deliveryStrategy;
    private String description;
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
