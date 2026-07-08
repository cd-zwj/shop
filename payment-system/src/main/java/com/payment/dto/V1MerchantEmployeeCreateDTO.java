package com.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商户端新增或重新启用员工请求。
 */
@Data
public class V1MerchantEmployeeCreateDTO {

    @NotNull(message = "平台用户ID不能为空")
    @Min(value = 1, message = "平台用户ID必须大于0")
    private Long platformUserId;

    @NotBlank(message = "员工角色不能为空")
    private String employeeRole;
}
