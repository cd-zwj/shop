package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商户端调整员工角色请求。
 */
@Data
public class V1MerchantEmployeeRoleUpdateDTO {

    @NotBlank(message = "员工角色不能为空")
    private String employeeRole;
}
