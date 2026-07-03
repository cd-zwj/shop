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

/**
 * 商户端辅助支撑服务。
 * <p>提供商户员工身份校验（requireEmployee）、当前用户可访问的活跃商户列表查询等功能，
 * 被商户端其他 Service 注入使用，作为统一的权限校验入口。</p>
 */
@Service
@RequiredArgsConstructor
public class V1MerchantSupportService {

    private final TenantEmployeeMapper tenantEmployeeMapper;
    private final TenantMapper tenantMapper;

    /**
     * 查询指定用户所属的所有活跃员工记录（关联租户未删除且已启用）。
     *
     * @param platformUserId 平台用户ID
     * @return 活跃员工记录列表，按创建时间升序排列
     */
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

    /**
     * 校验用户是否为指定租户的有效员工，同时校验租户状态，不满足则抛出异常。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @return 校验通过的员工记录
     * @throws BusinessException 当用户无权访问该商户，或商户不存在/已停用时抛出
     */
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

    /**
     * 获取当前用户可访问的所有商户列表（含商户名称和员工角色）。
     *
     * @param platformUserId 平台用户ID
     * @return 可访问的商户VO列表，包含租户ID、商户名称、员工角色
     */
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
