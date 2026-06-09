package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分兑换商品视图对象。
 */
@Data
public class ExchangeProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String productName;
    private String productImage;
    private Integer pointsRequired;
    private Integer stock;
    private Integer exchangeLimit;
    private String description;
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
