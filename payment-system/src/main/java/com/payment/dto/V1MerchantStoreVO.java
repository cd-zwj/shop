package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class V1MerchantStoreVO {

    private Long id;

    private String storeNo;

    private Long tenantId;

    private String storeName;

    private String storeType;

    private String contactName;

    private String contactPhone;

    private String province;

    private String city;

    private String district;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private BigDecimal rating;

    private String businessHours;

    private String serviceTags;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
