package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户配置实体
 */
@Data
@TableName("tenant_config")
public class TenantConfig implements Serializable {
    private static final long serialVersionUID = 1L;

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
