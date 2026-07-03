package com.payment.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 商户端卡密批量上传请求参数。
 */
@Data
public class V1MerchantCardKeyUploadDTO {

    /** 待上传的卡密编码列表 */
    @NotEmpty(message = "卡密列表不能为空")
    private List<String> codes;
}
