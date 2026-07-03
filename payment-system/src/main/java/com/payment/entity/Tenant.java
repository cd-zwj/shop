package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 租户实体，对应 tenant 表。
 * <p>表示平台中的商户/租户，是多租户架构的核心主表。
 * 每个租户拥有独立的业务数据隔离（通过 tenant_id 行级隔离），
 * 对应 B 端商户管理后台的一套完整业务空间。</p>
 */
@Data
@TableName("tenant")
public class Tenant implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增，同时作为 tenant_id 用于行级数据隔离 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户编码（唯一）
     */
    private String tenantCode;

    /**
     * 租户名称
     */
    private String name;

    /** 联系人姓名，商户的主要对接人 */
    private String contact;

    /** 联系电话，用于商户沟通和紧急联系 */
    private String phone;

    /** 商户地址 */
    private String address;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /** 逻辑删除标记：0-未删除，1-已删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
