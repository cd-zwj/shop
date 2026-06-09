package com.payment.vo;

import com.payment.entity.TenantConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 租户配置视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantConfigVO {

    private Long id;
    private Long tenantId;
    private String configKey;
    private String configValue;
    private String configType;
    private String description;
    private String createTime;
    private String updateTime;

    public static TenantConfigVO from(TenantConfig config) {
        if (config == null) {
            return null;
        }
        return TenantConfigVO.builder()
                .id(config.getId())
                .tenantId(config.getTenantId())
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .configType(config.getConfigType())
                .description(config.getDescription())
                .createTime(VoConverterUtil.formatTime(config.getCreateTime()))
                .updateTime(VoConverterUtil.formatTime(config.getUpdateTime()))
                .build();
    }
}
