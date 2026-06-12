package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 门店实体 — 数据预埋，暂不建设。
 * <p>
 * 当前状态（2026-06-12）：实体和 Mapper 存在但完全孤立，
 * 无 Service/Controller 业务代码引用。
 * SalesOrder 和 Product 已预留 store_id 字段，但当前业务代码未读写该字段，
 * 未形成门店业务闭环。多门店能力是后续扩展，当前以单门店商户为主。
 *
 * @see com.payment.mapper.StoreMapper
 */
@Data
@TableName("store")
public class Store implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String storeNo;

    private Long tenantId;

    private String storeName;

    private String storeType;

    private String contactName;

    private String contactPhone;

    private String province;

    private String city;

    private String district;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private BigDecimal rating;

    private String businessHours;

    private String serviceTags;

    private Integer status;

    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
