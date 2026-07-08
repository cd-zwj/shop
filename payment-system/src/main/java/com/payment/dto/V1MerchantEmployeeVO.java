package com.payment.dto;

import com.payment.entity.PlatformUser;
import com.payment.entity.TenantEmployee;
import lombok.Data;

import java.time.LocalDateTime;

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
    private Integer status;
    private String username;
    private String phone;
    private String email;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static V1MerchantEmployeeVO from(TenantEmployee employee, PlatformUser user) {
        V1MerchantEmployeeVO vo = new V1MerchantEmployeeVO();
        vo.setId(employee.getId());
        vo.setTenantId(employee.getTenantId());
        vo.setPlatformUserId(employee.getPlatformUserId());
        vo.setEmployeeNo(employee.getEmployeeNo());
        vo.setEmployeeRole(employee.getEmployeeRole());
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
