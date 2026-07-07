package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户收货地址数据传输对象，用于新增和编辑收货地址请求。
 */
@Data
public class UserShippingAddressDTO {

    /** 收货人姓名 */
    @NotBlank(message = "收货人不能为空")
    @Size(max = 50, message = "收货人不能超过 50 个字符")
    private String receiverName;

    /** 收货人手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Size(max = 20, message = "手机号不能超过 20 个字符")
    private String phone;

    /** 省份 */
    @Size(max = 50, message = "省份不能超过 50 个字符")
    private String province;

    /** 城市 */
    @NotBlank(message = "城市不能为空")
    @Size(max = 50, message = "城市不能超过 50 个字符")
    private String city;

    /** 区/县 */
    @Size(max = 50, message = "区县不能超过 50 个字符")
    private String district;

    /** 详细地址 */
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址不能超过 255 个字符")
    private String detail;

    /** 是否设为默认地址 */
    private Boolean isDefault;
}
