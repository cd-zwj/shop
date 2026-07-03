package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户收货地址实体类，对应数据库表 user_shipping_address。
 * <p>存储 C 端用户的配送地址信息，支持多地址管理和默认地址设置。</p>
 */
@Data
@TableName("user_shipping_address")
public class UserShippingAddress implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的平台用户ID */
    private Long platformUserId;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人手机号码 */
    private String phone;

    /** 省份/直辖市 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细街道地址 */
    private String detail;

    /** 是否默认地址：0-否，1-是（同一用户仅允许一个默认地址） */
    private Integer isDefault;

    /** 软删除标记：0-正常，1-已删除 */
    private Integer deleted;

    /** 地址创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;
}
