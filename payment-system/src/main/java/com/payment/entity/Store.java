package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 门店实体，对应数据库表 store。
 * <p>存储商户线下门店的基础信息，包括地理位置、营业时间、服务标签等。
 * Product / SalesOrder 通过 store_id 关联门店主数据。</p>
 *
 * @see com.payment.mapper.StoreMapper
 */
@Data
@TableName("store")
public class Store implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 门店主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门店编号，系统生成的唯一编码 */
    private String storeNo;

    /** 租户ID，用于多租户数据隔离 */
    private Long tenantId;

    /** 门店名称 */
    private String storeName;

    /** 门店类型（如：旗舰店、标准店、加盟店等） */
    private String storeType;

    /** 联系人姓名 */
    private String contactName;

    /** 联系人手机号 */
    private String contactPhone;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细地址 */
    private String address;

    /** 经度，用于LBS地理计算（如附近门店查询） */
    private BigDecimal longitude;

    /** 纬度，用于LBS地理计算（如附近门店查询） */
    private BigDecimal latitude;

    /** 门店评分，取值范围 0.00 ~ 5.00 */
    private BigDecimal rating;

    /** 营业时间描述（如：09:00-21:00） */
    private String businessHours;

    /** 服务标签（JSON数组或逗号分隔），如：支持配送,免费WiFi,停车 */
    private String serviceTags;

    /** 门店状态：0-关闭/停业，1-正常营业 */
    private Integer status;

    /** 逻辑删除标记：0-未删除，1-已删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
