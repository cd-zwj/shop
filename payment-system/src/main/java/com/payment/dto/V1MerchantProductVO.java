package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class V1MerchantProductVO {

    private Long id;

    private Long tenantId;

    private String productCode;

    private String name;

    private BigDecimal price;

    private String unit;

    private String category;

    private String description;

    private String imageUrl;

    private Integer stock;

    /**
     * active / inactive / out_of_stock
     */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
