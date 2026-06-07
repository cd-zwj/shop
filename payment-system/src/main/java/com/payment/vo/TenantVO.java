package com.payment.vo;

import com.payment.entity.Tenant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户端商户（租户）列表视图对象，隐藏 deleted 等内部字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantVO {

    private Long id;
    private String tenantCode;
    private String name;
    private String contact;
    private String phone;
    private String address;
    private Integer status;
    private String createTime;

    public static TenantVO from(Tenant tenant) {
        if (tenant == null) {
            return null;
        }
        return TenantVO.builder()
                .id(tenant.getId())
                .tenantCode(tenant.getTenantCode())
                .name(tenant.getName())
                .contact(tenant.getContact())
                .phone(tenant.getPhone())
                .address(tenant.getAddress())
                .status(tenant.getStatus())
                .createTime(VoConverterUtil.formatTime(tenant.getCreateTime()))
                .build();
    }
}
