package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VirtualProductTypeUpsertDTO {
    @NotBlank(message = "类型编码不能为空")
    private String typeCode;
    @NotBlank(message = "类型名称不能为空")
    private String typeName;
    @NotBlank(message = "交付策略不能为空")
    private String deliveryStrategy;
    private String description;
    private Integer status;
    private Integer sortOrder;
}
