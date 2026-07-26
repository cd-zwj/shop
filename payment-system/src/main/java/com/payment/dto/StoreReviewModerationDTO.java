package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 平台对门店评价的可见性处理。 */
@Data
public class StoreReviewModerationDTO {
    @NotNull(message = "可见状态不能为空")
    private Boolean visible;

    @NotBlank(message = "处理说明不能为空")
    @Size(max = 500, message = "处理说明不能超过500个字符")
    private String remark;
}
