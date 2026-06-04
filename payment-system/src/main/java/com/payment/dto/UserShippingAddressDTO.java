package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户收货地址数据对象，用于承载新增和编辑地址请求。
 */
@Data
public class UserShippingAddressDTO {

    @NotBlank(message = "收货人不能为空")
    @Size(max = 50, message = "收货人不能超过 50 个字符")
    private String receiverName;

    @NotBlank(message = "手机号不能为空")
    @Size(max = 20, message = "手机号不能超过 20 个字符")
    private String phone;

    @Size(max = 50, message = "省份不能超过 50 个字符")
    private String province;

    @NotBlank(message = "城市不能为空")
    @Size(max = 50, message = "城市不能超过 50 个字符")
    private String city;

    @Size(max = 50, message = "区县不能超过 50 个字符")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址不能超过 255 个字符")
    private String detail;

    private Boolean isDefault;
}
