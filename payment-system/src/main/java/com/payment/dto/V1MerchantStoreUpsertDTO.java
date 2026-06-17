package com.payment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class V1MerchantStoreUpsertDTO {

    private String storeNo;

    @NotBlank(message = "门店名称不能为空")
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

    private String businessHours;

    private String serviceTags;

    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;
}
