package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VirtualProductCategoryUpsertDTO {
    @NotNull(message = "虚拟商品类型不能为空")
    private Long typeId;
    @NotBlank(message = "分类编码不能为空")
    private String categoryCode;
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;
    private Long parentId;
    private String description;
    private Integer status;
    private Integer sortOrder;
}
