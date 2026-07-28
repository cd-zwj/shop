package com.payment.dto;

import com.payment.entity.PlatformUser;
import com.payment.entity.TenantEmployee;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商户端员工列表视图。
 */
@Data
public class V1MerchantEmployeeVO {

    private Long id;
    private Long tenantId;
    private Long platformUserId;
    private String employeeNo;
    private String employeeRole;
    private String storeScopeType;
    private List<Long> storeIds;
    private Integer status;
    private String username;
    private String phone;
    private String email;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static V1MerchantEmployeeVO from(TenantEmployee employee, PlatformUser user) {
        return from(employee, user, List.of());
    }

    public static V1MerchantEmployeeVO from(TenantEmployee employee, PlatformUser user, List<Long> storeIds) {
        V1MerchantEmployeeVO vo = new V1MerchantEmployeeVO();
        vo.setId(employee.getId());
        vo.setTenantId(employee.getTenantId());
        vo.setPlatformUserId(employee.getPlatformUserId());
        vo.setEmployeeNo(employee.getEmployeeNo());
        vo.setEmployeeRole(employee.getEmployeeRole());
        vo.setStoreScopeType(employee.getStoreScopeType());
        vo.setStoreIds(storeIds == null ? List.of() : List.copyOf(storeIds));
        vo.setStatus(employee.getStatus());
        vo.setCreateTime(employee.getCreateTime());
        vo.setUpdateTime(employee.getUpdateTime());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setPhone(user.getPhone());
            vo.setEmail(user.getEmail());
        }
        return vo;
    }
}
