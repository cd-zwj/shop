package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.dto.V1MerchantTenantVO;
import com.payment.entity.Tenant;
import com.payment.entity.TenantEmployee;
import com.payment.mapper.TenantEmployeeMapper;
import com.payment.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class V1MerchantSupportService {

    private final TenantEmployeeMapper tenantEmployeeMapper;
    private final TenantMapper tenantMapper;

    public List<TenantEmployee> listActiveEmployees(Long platformUserId) {
        return tenantEmployeeMapper.selectList(new LambdaQueryWrapper<TenantEmployee>()
                .eq(TenantEmployee::getPlatformUserId, platformUserId)
                .eq(TenantEmployee::getStatus, 1))
                .stream()
                .filter(employee -> {
                    Tenant tenant = tenantMapper.selectById(employee.getTenantId());
                    return tenant != null
                            && (tenant.getDeleted() == null || tenant.getDeleted() == 0)
                            && tenant.getStatus() != null
                            && tenant.getStatus() == 1;
                })
                .sorted(Comparator.comparing(TenantEmployee::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public TenantEmployee requireEmployee(Long tenantId, Long platformUserId) {
        TenantEmployee employee = tenantEmployeeMapper.selectOne(new LambdaQueryWrapper<TenantEmployee>()
                .eq(TenantEmployee::getTenantId, tenantId)
                .eq(TenantEmployee::getPlatformUserId, platformUserId)
                .eq(TenantEmployee::getStatus, 1));
        if (employee == null) {
            throw new BusinessException("当前用户无权访问该商户");
        }

        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || (tenant.getDeleted() != null && tenant.getDeleted() == 1) || tenant.getStatus() == null || tenant.getStatus() == 0) {
            throw new BusinessException("商户不存在或已停用");
        }
        return employee;
    }

    public List<V1MerchantTenantVO> listAccessibleTenants(Long platformUserId) {
        return listActiveEmployees(platformUserId).stream()
                .map(employee -> {
                    Tenant tenant = tenantMapper.selectById(employee.getTenantId());
                    V1MerchantTenantVO vo = new V1MerchantTenantVO();
                    vo.setTenantId(employee.getTenantId());
                    vo.setTenantName(tenant == null ? null : tenant.getName());
                    vo.setEmployeeRole(employee.getEmployeeRole());
                    return vo;
                })
                .toList();
    }
}
