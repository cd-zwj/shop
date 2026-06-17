package com.payment.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class V1MerchantCardKeyUploadDTO {

    @NotEmpty(message = "卡密列表不能为空")
    private List<String> codes;
}
