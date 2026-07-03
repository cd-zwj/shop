package com.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VirtualProductCategoryVO {
    private Long id;
    private Long tenantId;
    private Long typeId;
    private String categoryCode;
    private String categoryName;
    private Long parentId;
    private String description;
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
