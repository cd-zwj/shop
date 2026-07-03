package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户配置实体，对应 tenant_config 表。
 * <p>以 Key-Value 形式存储每个租户的个性化配置项，
 * 支持系统预置配置和商户自定义配置，实现租户级别的差异化运营。</p>
 */
@Data
@TableName("tenant_config")
public class TenantConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 配置键: PAYMENT_CHANNEL/FEATURE_TOGGLE/BRAND_NAME等
     */
    private String configKey;

    /**
     * 配置值（JSON格式）
     */
    private String configValue;

    /**
     * 配置类型: SYSTEM/CUSTOM
     */
    private String configType;

    /**
     * 配置说明
     */
    private String description;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
