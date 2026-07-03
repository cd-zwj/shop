package com.payment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商户端门店创建/更新请求参数。
 */
@Data
public class V1MerchantStoreUpsertDTO {

    /** 门店编号（不传则自动生成） */
    private String storeNo;

    /** 门店名称 */
    @NotBlank(message = "门店名称不能为空")
    private String storeName;

    /** 门店类型（如：直营店、加盟店） */
    private String storeType;

    /** 联系人姓名 */
    private String contactName;

    /** 联系人电话 */
    private String contactPhone;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细地址 */
    private String address;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 营业时间（如：09:00-21:00） */
    private String businessHours;

    /** 服务标签（JSON 数组字符串，如：["停车","WiFi"]） */
    private String serviceTags;

    /** 门店状态（0-关闭，1-营业） */
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;
}
