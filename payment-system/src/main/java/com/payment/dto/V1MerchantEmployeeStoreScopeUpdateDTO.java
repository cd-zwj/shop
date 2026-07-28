package com.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class V1MerchantEmployeeStoreScopeUpdateDTO {

    @NotBlank(message = "门店范围类型不能为空")
    private String storeScopeType;

    @Size(max = 100, message = "单个员工最多分配100家门店")
    private List<@NotNull(message = "门店ID不能为空") @Min(value = 1, message = "门店ID必须大于0") Long> storeIds;
}
