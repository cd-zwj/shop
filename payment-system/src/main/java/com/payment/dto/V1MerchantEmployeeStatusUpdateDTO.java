package com.payment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商户端启用或禁用员工请求。
 */
@Data
public class V1MerchantEmployeeStatusUpdateDTO {

    @NotNull(message = "员工状态不能为空")
    @Min(value = 0, message = "员工状态只能为0或1")
    @Max(value = 1, message = "员工状态只能为0或1")
    private Integer status;
}
