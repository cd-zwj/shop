package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户端门店详情视图对象，展示门店的完整信息。
 */
@Data
public class V1MerchantStoreVO {

    /** 门店 ID */
    private Long id;

    /** 门店编号 */
    private String storeNo;

    /** 所属租户（商户）ID */
    private Long tenantId;

    /** 门店名称 */
    private String storeName;

    /** 门店类型 */
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

    /** 门店评分 */
    private BigDecimal rating;

    /** 营业时间 */
    private String businessHours;

    /** 服务标签 */
    private String serviceTags;

    /** 门店状态（0-关闭，1-营业） */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
